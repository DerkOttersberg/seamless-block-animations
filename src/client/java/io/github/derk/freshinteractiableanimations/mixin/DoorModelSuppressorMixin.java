package io.github.derk.freshinteractiableanimations.mixin;

import io.github.derk.freshinteractiableanimations.AnimatedBlockSnapshot;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BlockModelRenderer.class)
public final class DoorModelSuppressorMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void freshInteractiableAnimations$suppressTrackedDoorModel(BlockRenderView world, List<?> parts, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, int overlay, CallbackInfo ci) {
        if (AnimatedBlockSnapshot.supports(state) && DoorAnimationTimeline.shouldSuppressAnimatedModel(pos, state)) {
            ci.cancel();
        }
    }
}
