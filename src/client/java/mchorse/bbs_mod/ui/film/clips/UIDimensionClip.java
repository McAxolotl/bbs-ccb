package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.overwrite.DimensionClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.modules.UIAngleModule;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.film.utils.UICameraUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dimension clip panel.
 *
 * <p>Provides editing for {@link DimensionClip}: target dimension input,
 * registered dimension selector dropdown, coordinate and angle modules, and context menu.</p>
 */
public class UIDimensionClip extends UIClip<DimensionClip>
{
    public UIPointModule point;
    public UIAngleModule angle;
    public UITextbox dimensionInput;
    public UIButton pickDimensionBtn;

    public UIDimensionClip(DimensionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    public static List<Link> getRegisteredDimensions()
    {
        Set<Link> set = new LinkedHashSet<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getNetworkHandler() != null)
        {
            Set<RegistryKey<World>> worldKeys = client.getNetworkHandler().getWorldKeys();
            if (worldKeys != null)
            {
                for (RegistryKey<World> key : worldKeys)
                {
                    Identifier id = key.getValue();
                    set.add(new Link(id.getNamespace(), id.getPath()));
                }
            }
        }

        if (client != null && client.world != null)
        {
            Identifier currentId = client.world.getRegistryKey().getValue();
            set.add(new Link(currentId.getNamespace(), currentId.getPath()));
        }

        if (set.isEmpty())
        {
            set.add(Link.create("minecraft:overworld"));
        }

        List<Link> list = new ArrayList<>(set);
        list.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        return list;
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.point = this.bind(new UIPointModule(this.editor), () -> this.point.fill(this.clip.position.getPoint()));
        this.angle = this.bind(new UIAngleModule(this.editor), () -> this.angle.fill(this.clip.position.getAngle()));

        this.dimensionInput = new UITextbox(1000, (text) ->
        {
            String trimmed = text.trim();
            if (!trimmed.isEmpty())
            {
                this.editor.editMultiple(this.clip.dimension, (v) -> v.set(Link.create(trimmed)));
            }
        });

        this.bind(this.dimensionInput, () ->
        {
            Link current = this.clip.dimension.get();
            this.dimensionInput.setText(current == null ? "" : current.toString());
        });

        this.pickDimensionBtn = new UIButton(UIKeys.CAMERA_PANELS_DIMENSION_PICK, (b) ->
        {
            List<Link> dimensions = getRegisteredDimensions();
            UISimpleContextMenu simpleMenu = new UISimpleContextMenu();

            simpleMenu.actions.scroll.scrollItemSize = 20;

            b.getContext().replaceContextMenu((menu) ->
            {
                menu.custom(simpleMenu);

                for (Link link : dimensions)
                {
                    boolean highlight = link.equals(this.clip.dimension.get());

                    menu.action(Icons.GLOBE, IKey.raw(link.toString()), highlight, () ->
                    {
                        this.editor.editMultiple(this.clip.dimension, (v) -> v.set(link));
                    });
                }
            });
        });
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            UIKeys.CAMERA_PANELS_DIMENSION_TITLE,
            this.dimensionInput,
            this.pickDimensionBtn
        ));
        this.panels.add(this.point, this.angle);
        this.panels.context((menu) -> UICameraUtils.positionContextMenu(menu, this.editor, this.clip.position));
    }

    @Override
    public void editClip(Position position)
    {
        this.clip.position.set(position);

        super.editClip(position);
    }
}
