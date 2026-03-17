package io.github.derk.freshinteractiableanimations.mixin;

import io.github.derk.freshinteractiableanimations.render.DoorSceneRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class WorldRenderAttachmentMixin {
    @Shadow
    private BufferBuilderStorage bufferBuilders;

    @Shadow
    private ClientWorld world;

    @Inject(method = "renderBlockEntities", at = @At("TAIL"), require = 0)
    private void freshInteractiableAnimations$renderAnimatedDoorLayer(MatrixStack ignoredMatrices, WorldRenderState worldRenderState, OrderedRenderCommandQueueImpl ignoredQueue, CallbackInfo ci) {
        if (this.world == null) {
            return;
        }

        VertexConsumerProvider.Immediate consumers = this.bufferBuilders.getEntityVertexConsumers();
        DoorSceneRenderer.renderAnimatedBlocks(this.world, new MatrixStack(), resolveCameraPosition(), consumers);
    }

    private static Vec3d resolveCameraPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null) {
            return Vec3d.ZERO;
        }

        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return Vec3d.ZERO;
        }

        return ((CameraPositionAccessor) camera).getCameraPosition();
    }
}
