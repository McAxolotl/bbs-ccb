package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.runtime.DimensionRuntimeController;
import mchorse.bbs_mod.forms.structure.StructureWand;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client mixin handling structure wand clicks and seamless cross-dimension transitions.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    public void wandAttack(CallbackInfoReturnable<Boolean> cir)
    {
        if (StructureWand.onAttack())
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    public void wandUse(CallbackInfo ci)
    {
        if (StructureWand.onUse())
        {
            ci.cancel();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci)
    {
        boolean isDashboard = UIScreen.getCurrentMenu() == BBSModClient.getDashboard();
        boolean isExternalFilm = BBSModClient.getFilms() != null
            && (!BBSModClient.getFilms().getControllers().isEmpty() || BBSModClient.getFilms().getRecorder() != null);
        boolean isCameraActive = BBSModClient.getCameraController().getCurrent() != null;
        boolean isSwitchingDim = DimensionRuntimeController.isSwitchingDimension();
        boolean isRecording = BBSModClient.getVideoRecorder() != null && BBSModClient.getVideoRecorder().isRecording();

        boolean isBBSActive = isDashboard || isExternalFilm || isCameraActive || isSwitchingDim || isRecording;

        if (isBBSActive && (screen instanceof DownloadingTerrainScreen || screen instanceof ProgressScreen))
        {
            ci.cancel();

            return;
        }

        if ((isDashboard || isRecording) && isSwitchingDim && screen == null)
        {
            ci.cancel();
        }
    }
}
