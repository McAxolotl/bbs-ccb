package mchorse.bbs_mod.mixin.client.iris;

import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTargets.class)
public class RenderTargetsMixin
{
    @Shadow(remap = false)
    private int currentDepthTexture;

    @Shadow(remap = false)
    private int cachedDepthBufferVersion;

    @Inject(method = "resizeIfNeeded", at = @At("HEAD"), remap = false)
    private void bbs$refreshSwappedDepthTexture(int depthBufferVersion, int depthTexture, int width, int height,
        DepthBufferFormat depthFormat, PackDirectives directives, CallbackInfoReturnable<Boolean> info)
    {
        /* BBS swaps the window framebuffer for its preview/export framebuffer. Their local
         * version counters can match even though their depth textures differ. Iris 1.7.2 only
         * checks the version here (unlike its final color pass), leaving stale depth attachments.
         * Invalidate the cached version so Iris reattaches depth through its normal update path.
         * This also handles returning to the window and pipelines created during a shader reload. */
        if (this.currentDepthTexture != depthTexture && this.cachedDepthBufferVersion == depthBufferVersion)
        {
            this.cachedDepthBufferVersion = depthBufferVersion - 1;
        }
    }
}
