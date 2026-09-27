package mchorse.bbs_mod.ui.dashboard.list;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.NaturalOrderComparator;
import java.util.HashSet;
import java.util.Set;

/** Folder navigation over the data manager's existing repository listing. */
public class UIDataFolderTree extends UIList<DataPath>
{
    private final UIDataPathList files;
    private final IKey rootTitle;
    private final Set<DataPath> folders = new HashSet<>();
    private final Set<DataPath> expanded = new HashSet<>();

    public UIDataFolderTree(UIDataPathList files, IKey rootTitle)
    {
        super(values -> { if (!values.isEmpty()) files.goTo(values.get(0)); });
        this.files = files;
        this.rootTitle = rootTitle;
        this.scroll.scrollItemSize = mchorse.bbs_mod.ui.textures.UIFolderTree.ROW;
        this.expanded.add(DataPath.EMPTY);
        files.onChanged(this::refresh);
        this.refresh();
    }

    public void refresh()
    {
        this.folders.clear();
        for (DataPath file : this.files.getHierarchy())
        {
            DataPath folder = file.folder ? file : file.getParent();
            while (!folder.strings.isEmpty())
            {
                this.folders.add(folder);
                folder = folder.getParent();
            }
        }
        DataPath current = this.files.getPath().copy();
        while (!current.strings.isEmpty())
        {
            current = current.getParent();
            this.expanded.add(current);
        }
        this.rebuild();
    }

    private void rebuild()
    {
        this.list.clear();
        this.append(DataPath.EMPTY);
        this.setCurrent(this.files.getPath());
        this.update();
    }

    private void append(DataPath folder)
    {
        this.list.add(folder);
        if (this.expanded.contains(folder))
        {
            this.folders.stream().filter(p -> p.startsWith(folder, 1))
                .sorted((a, b) -> NaturalOrderComparator.compare(true, a.toString(), b.toString()))
                .forEach(this::append);
        }
    }

    @Override
    protected int indent(DataPath folder)
    {
        return folder.size() * 10;
    }

    @Override
    protected Boolean branch(DataPath folder)
    {
        return this.folders.stream().anyMatch(p -> p.startsWith(folder, 1)) ? this.expanded.contains(folder) : null;
    }

    @Override
    protected void toggle(DataPath folder)
    {
        if (!this.expanded.remove(folder)) this.expanded.add(folder);
        this.rebuild();
    }

    @Override
    protected String elementToString(UIContext context, int index, DataPath folder)
    {
        return folder.strings.isEmpty() ? this.rootTitle.get() : folder.getLast();
    }

    @Override
    protected void renderElementPart(UIContext context, DataPath folder, int i, int x, int y, boolean hover, boolean selected)
    {
        this.renderArrow(context, folder, x, y, hover || selected);
        int left = x + this.rowContentX(folder) + ARROW_SLOT;
        context.batcher.icon(Icons.FOLDER, RowStyle.iconColor(hover || selected), left, y + this.rowHeight() / 2F, 0F, 0.5F);
        String label = context.batcher.getFont().limitToWidth(this.elementToString(context, i, folder), Math.max(1, this.area.ex() - left - 20));
        context.batcher.textShadow(label, left + 18, y + (this.rowHeight() - context.batcher.getFont().getHeight()) / 2, RowStyle.textColor(hover || selected));
    }
}
