package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.cubic.spline.*;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.*;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.context.MenuVerb;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.*;
import static mchorse.bbs_mod.ui.forms.editors.panels.UIModelSplineFormPanel.key;

/** Selection, coordinates and structural actions, shared by paths and IK. Keys disable topology edits. */
public class UISplinePointsEditor extends UIElement
{
    private static final String CLIPBOARD = "_CopySplinePoints";
    public final UISplinePointList points;
    public final UIPropTransform position;
    private final Supplier<SplineSource> source;
    private final Function<String, Transform> read;
    private final Supplier<Vector3f> initial;
    private final boolean structure;
    private final Set<String> selected = new LinkedHashSet<>();
    private String pointId = "";

    public UISplinePointsEditor(Supplier<SplineSource> source, Function<String, Transform> read,
        UIPropTransform position, Supplier<Vector3f> initial, boolean structure)
    {
        this.source = source;
        this.read = read;
        this.position = position;
        this.initial = initial;
        this.structure = structure;
        this.points = new UISplinePointList(this::selectPoints)
        {
            @Override public UIContextMenu createContextMenu(UIContext context)
            {
                int i = this.getIndexAtCursor(context);
                if (i >= 0 && !this.getCurrent().contains(this.getList().get(i))) UISplinePointsEditor.this.select(this.getList().get(i));
                return super.createContextMenu(context);
            }
            @Override protected boolean onDelete(List<String> items) { return UISplinePointsEditor.this.removeSelected(); }
        };
        this.column(UIConstants.MARGIN).vertical().stretch();
        if (structure)
        {
            UIElement header = UI.row(UIConstants.MARGIN, 0, UIConstants.CONTROL_HEIGHT,
                UI.label(key("points"), UIConstants.CONTROL_HEIGHT).labelAnchor(0, 0.5F),
                new UIIcon(Icons.ADD, b -> this.addPoint()).wh(20, 20).tooltip(UIKeys.GENERAL_ADD),
                new UIIcon(Icons.REMOVE, b -> this.removeSelected()).wh(20, 20).tooltip(UIKeys.GENERAL_REMOVE));
            header.context(this::menu);
            this.points.context(this::menu);
            this.add(header);
        }
        this.position.noScale().setRotationVisible(false);
        this.add(this.points, this.position);
    }

    public String pointId() { return this.pointId; }
    public Set<String> selected() { return Set.copyOf(this.selected); }
    public SplinePoint point()
    {
        SplineSource source = this.source.get();
        return source == null ? null : source.points().get(this.pointId);
    }
    public void endEdit()
    {
        if (this.position.isEditing()) this.position.endGesture();
        this.position.setTransform(null);
    }
    public void select(String id)
    {
        this.points.setCurrent(id);
        this.selectPoints(id.isEmpty() ? List.of() : List.of(id));
    }
    public void selectInViewport(String id, boolean extend)
    {
        List<String> ids = extend ? new ArrayList<>(this.selected) : new ArrayList<>();
        if (!extend || !ids.remove(id)) ids.add(id);
        this.points.setCurrent(ids);
        this.selectPoints(ids);
    }
    private void selectPoints(List<String> ids)
    {
        this.endEdit();
        this.selected.clear();
        this.selected.addAll(ids);
        String anchor = this.points.selection.getAnchor();
        this.pointId = ids.isEmpty() ? ""
            : anchor != null && ids.contains(anchor) ? anchor : ids.get(ids.size() - 1);
        this.refreshPosition();
        this.resize();
        if (this.getParent() != null) this.getParent().resize();
    }
    private void refreshPosition()
    {
        this.position.setVisible(this.point() != null);
        this.position.setTransform(this.point() == null ? null : this.read.apply(this.pointId));
    }
    public void refresh()
    {
        SplineSource source = this.source.get();
        List<String> ids = new ArrayList<>();
        if (source != null) for (SplinePoint point : source.points().getAllTyped()) ids.add(point.getId());
        this.selected.retainAll(ids);
        if (this.selected.isEmpty() && !ids.isEmpty()) this.selected.add(ids.get(0));
        if (!ids.contains(this.pointId)) this.pointId = this.selected.isEmpty() ? "" : this.selected.iterator().next();
        this.points.setList(ids);
        this.points.setCurrent(new ArrayList<>(this.selected));
        this.points.h(Math.max(1, Math.min(6, ids.size())) * this.points.rowHeight());
        this.refreshPosition();
        this.resize();
    }
    private void edit(Consumer<ValueSplinePoints> edit)
    {
        SplineSource source = this.source.get();
        if (!this.structure || source == null) return;
        this.endEdit();
        BaseValue.edit(source.points(), IValueListener.FLAG_UNMERGEABLE, edit);
        this.refresh();
        if (this.getParent() != null) this.getParent().resize();
    }
    private int index() { return this.points.getList().indexOf(this.pointId); }
    private void menu(ContextMenuManager menu)
    {
        if (this.source.get() == null) return;
        menu.icon(MenuVerb.ADD, this::addPoint);
        menu.icon(MenuVerb.REMOVE, this::removeSelected).enabled(!this.selected.isEmpty());
        menu.icon(MenuVerb.COPY, this::copy);
        menu.icon(MenuVerb.PASTE, this::paste);
        if (this.points.getList().size() > 2) menu.action(Icons.ALL_DIRECTIONS, key("distribute_points"), this::distribute);
        if (this.selected.size() != 1) return;
        int i = this.index();
        if (i > 0) menu.action(Icons.MOVE_UP, key("up"), () -> this.move(-1));
        if (i + 1 < this.points.getList().size()) menu.action(Icons.MOVE_DOWN, key("down"), () -> this.move(1));
    }
    private void addPoint()
    {
        SplineSource source = this.source.get();
        if (source == null) return;
        List<SplinePoint> points = source.points().getAllTyped();
        int i = Math.max(0, this.index() + 1);
        Vector3f position = points.isEmpty() ? this.initial.get() : new Vector3f(points.get(Math.max(0, i - 1)).position.getOriginalValue().translate);
        if (position == null) return;
        if (i < points.size()) position.lerp(points.get(i).position.getOriginalValue().translate, 0.5F);
        else if (points.size() > 1) position.mul(2).sub(points.get(points.size() - 2).position.getOriginalValue().translate);
        this.insert(i - 1, position);
    }
    public void insert(int after, Vector3f position)
    {
        SplineSource source = this.source.get();
        if (source == null || !this.structure || !position.isFinite() || after < -1 || after >= source.points().getAllTyped().size()) return;
        SplinePoint point = new SplinePoint("");
        point.position.getOriginalValue().translate.set(position);
        this.edit(list -> list.add(after + 1, point));
        this.select(point.getId());
    }
    public boolean removeSelected()
    {
        if (!this.structure || this.source.get() == null || this.selected.isEmpty()) return false;
        Set<String> ids = this.selected();
        this.edit(list -> list.getAllTyped().removeIf(point -> ids.contains(point.getId())));
        return true;
    }
    private void move(int offset)
    {
        int from = this.index(), to = from + offset;
        if (from >= 0 && to >= 0 && to < this.points.getList().size()) this.edit(list -> Collections.swap(list.getAllTyped(), from, to));
    }
    private void copy()
    {
        ListType positions = new ListType();
        for (SplinePoint point : this.source.get().points().getAllTyped()) positions.add(DataStorageUtils.vector3fToData(point.position.getOriginalValue().translate));
        MapType data = new MapType();
        data.put("points", positions);
        Window.setClipboard(data, CLIPBOARD);
    }
    private void paste()
    {
        MapType data = Window.getClipboardMap(CLIPBOARD);
        if (data == null || !data.has("points", BaseType.TYPE_LIST)) return;
        List<Vector3f> positions = new ArrayList<>();
        for (BaseType entry : data.getList("points"))
        {
            if (!entry.isList() || entry.asList().size() != 3) return;
            for (BaseType value : entry.asList()) if (!BaseType.isNumeric(value)) return;
            Vector3f position = DataStorageUtils.vector3fFromData(entry.asList());
            if (!position.isFinite()) return;
            positions.add(position);
        }
        this.edit(list ->
        {
            List<SplinePoint> points = list.getAllTyped();
            while (points.size() < positions.size()) list.add(new SplinePoint(""));
            while (points.size() > positions.size()) points.remove(points.size() - 1);
            for (int i = 0; i < positions.size(); i++) points.get(i).position.getOriginalValue().translate.set(positions.get(i));
        });
    }
    private void distribute()
    {
        this.edit(list ->
        {
            List<SplinePoint> points = list.getAllTyped();
            if (points.size() < 3) return;
            Vector3f a = new Vector3f(points.get(0).position.getOriginalValue().translate);
            Vector3f b = new Vector3f(points.get(points.size() - 1).position.getOriginalValue().translate);
            for (int i = 1; i < points.size() - 1; i++) points.get(i).position.getOriginalValue().translate.set(a).lerp(b, i / (float) (points.size() - 1));
        });
    }
    @Override public void render(UIContext context)
    {
        if (!this.position.isUserEditing()) this.refreshPosition();
        super.render(context);
    }
}
