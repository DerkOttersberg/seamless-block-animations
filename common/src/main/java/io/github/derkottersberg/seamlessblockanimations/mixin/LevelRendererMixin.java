package io.github.derkottersberg.seamlessblockanimations.mixin;

import io.github.derkottersberg.seamlessblockanimations.render.AnimatedBlockRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void seamlessBlockAnimations$submitAnimatedBlocks(
        LevelRenderState renderState,
        SubmitNodeCollector collector,
        boolean outlines,
        CallbackInfo ci
    ) {
        AnimatedBlockRenderer.submit(renderState, collector);
    }
}
