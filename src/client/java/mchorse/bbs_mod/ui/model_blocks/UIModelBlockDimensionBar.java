package mchorse.bbs_mod.ui.model_blocks;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.runtime.DimensionRuntimeController;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.clips.UIDimensionClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Model block dimension bar.
 *
 * <p>Placed above the model block search list, allowing quick dimension teleportation
 * directly within the model block editor panel.</p>
 */
public class UIModelBlockDimensionBar extends UIElement
{
    private final UIButton dimensionBtn;
    private String lastDim = "";

    public UIModelBlockDimensionBar()
    {
        this.dimensionBtn = new UIButton(UIKeys.CAMERA_PANELS_DIMENSION_PICK, this::openDimensionMenu);
        this.dimensionBtn.relative(this).w(1F).h(20);

        this.h(20);
        this.add(this.dimensionBtn);
    }

    private void openDimensionMenu(UIButton button)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null)
        {
            return;
        }

        List<Link> dimensions = UIDimensionClip.getRegisteredDimensions();
        UISimpleContextMenu simpleMenu = new UISimpleContextMenu();

        simpleMenu.actions.scroll.scrollItemSize = 20;

        Identifier currentDim = mc.world.getRegistryKey().getValue();

        button.getContext().replaceContextMenu((menu) ->
        {
            menu.custom(simpleMenu);

            for (Link link : dimensions)
            {
                boolean highlight = link.toString().equals(currentDim.toString());

                menu.action(Icons.GLOBE, IKey.raw(link.toString()), highlight, () ->
                {
                    if (!link.toString().equals(currentDim.toString()))
                    {
                        Identifier targetDim = Identifier.tryParse(link.toString());

                        if (targetDim != null)
                        {
                            this.teleportToDimension(targetDim);
                        }
                    }
                });
            }
        });
    }

    private void teleportToDimension(Identifier targetDim)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        if (player == null)
        {
            return;
        }

        Camera camera = BBSModClient.getCameraController().camera;
        double x = camera != null ? camera.position.x : player.getX();
        double y = camera != null ? camera.position.y : player.getY();
        double z = camera != null ? camera.position.z : player.getZ();
        float yaw = camera != null ? (float) Math.toDegrees(camera.rotation.y) : player.getYaw();
        float pitch = camera != null ? (float) Math.toDegrees(camera.rotation.x) : player.getPitch();

        DimensionRuntimeController.requestDimensionSwitch(targetDim, x, y, z, yaw, pitch);
    }

    @Override
    public void render(UIContext context)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world != null)
        {
            String currentDim = mc.world.getRegistryKey().getValue().toString();

            if (!currentDim.equals(this.lastDim))
            {
                this.lastDim = currentDim;
                this.dimensionBtn.tooltip(IKey.raw(currentDim));
            }
        }

        super.render(context);
    }
}
