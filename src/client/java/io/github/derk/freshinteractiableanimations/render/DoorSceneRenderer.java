package io.github.derk.freshinteractiableanimations.render;

import io.github.derk.freshinteractiableanimations.AnimatedModelRenderContext;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import io.github.derk.freshinteractiableanimations.HingeKinematics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

@Environment(EnvType.CLIENT)
public final class DoorSceneRenderer {
    private static final double CAMERA_BIAS = 0.0005;

    private DoorSceneRenderer() {
    }

    public static void renderAnimatedBlocks(ClientWorld world, MatrixStack matrices, Vec3d cameraPosition, VertexConsumerProvider consumers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || world == null || consumers == null) {
            return;
        }

        BlockRenderManager renderer = client.getBlockRenderManager();
        DoorAnimationTimeline.visitRunningTransitions((anchorPos, slice) -> drawAnimatedBlock(world, renderer, consumers, matrices, cameraPosition, anchorPos, slice));
    }

    private static void drawAnimatedBlock(ClientWorld world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos anchorPos, DoorAnimationTimeline.MotionSlice slice) {
        Float progress = DoorAnimationTimeline.sampleProgress(anchorPos);
        if (progress == null) {
            return;
        }

        switch (slice.kind) {
            case DOOR -> drawDoorPair(world, renderer, consumers, matrices, cameraPosition, anchorPos, slice, progress);
            case TRAPDOOR -> drawTrapdoor(world, renderer, consumers, matrices, cameraPosition, anchorPos, slice, progress);
            case FENCE_GATE -> drawFenceGate(world, renderer, consumers, matrices, cameraPosition, anchorPos, slice, progress);
        }
    }

    private static void drawDoorPair(ClientWorld world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos basePos, DoorAnimationTimeline.MotionSlice slice, float progress) {
        BlockState existingState = world.getBlockState(basePos);
        if (!(existingState.getBlock() instanceof DoorBlock)) {
            return;
        }

        BlockState lowerClosedState = slice.closedState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperClosedState = slice.closedState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        Direction facing = lowerClosedState.get(Properties.HORIZONTAL_FACING);
        DoorHinge hinge = lowerClosedState.get(Properties.DOOR_HINGE);
        float currentAngle = HingeKinematics.sampleDoorAngleDegrees(facing, hinge, slice.opening, progress);

        drawSingleLeaf(world, renderer, consumers, matrices, cameraPosition, basePos, lowerClosedState, currentAngle, slice);
        drawSingleLeaf(world, renderer, consumers, matrices, cameraPosition, basePos.up(), upperClosedState, currentAngle, slice);
    }

    private static void drawTrapdoor(ClientWorld world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos pos, DoorAnimationTimeline.MotionSlice slice, float progress) {
        BlockState existingState = world.getBlockState(pos);
        if (!(existingState.getBlock() instanceof TrapdoorBlock)) {
            return;
        }

        BlockState closedState = slice.closedState;
        BlockHalf half = closedState.get(Properties.BLOCK_HALF);
        HingeKinematics.TrapdoorPose pose = HingeKinematics.describeTrapdoorPose(
            closedState.get(Properties.HORIZONTAL_FACING),
            half,
            progress
        );
        float closedCenterY = half == BlockHalf.TOP ? 1.0f - HingeKinematics.PANEL_THICKNESS * 0.5f : HingeKinematics.PANEL_THICKNESS * 0.5f;

        matrices.push();

        Vec3d translated = new Vec3d(pos.getX() - cameraPosition.x, pos.getY() - cameraPosition.y, pos.getZ() - cameraPosition.z);
        Vec3d bias = biasTowardCamera(cameraPosition, pos);
        matrices.translate(translated.x + bias.x, translated.y, translated.z + bias.z);
        matrices.translate(pose.centerX(), pose.centerY(), pose.centerZ());

        if (pose.axis() == HingeKinematics.Axis.X) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.degrees()));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.degrees()));
        }

        matrices.translate(-0.5, -closedCenterY, -0.5);

        try {
            renderer.renderBlockAsEntity(closedState, matrices, consumers, WorldRenderer.getLightmapCoordinates(world, pos), OverlayTexture.DEFAULT_UV);
        } catch (RuntimeException ignored) {
        }

        matrices.pop();
    }

    private static void drawFenceGate(ClientWorld world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos pos, DoorAnimationTimeline.MotionSlice slice, float progress) {
        BlockState existingState = world.getBlockState(pos);
        if (!(existingState.getBlock() instanceof FenceGateBlock)) {
            return;
        }

        BlockState closedState = slice.closedState;
        Direction facing = closedState.get(Properties.HORIZONTAL_FACING);

        drawFenceGateLeaf(world, renderer, consumers, matrices, cameraPosition, pos, closedState, HingeKinematics.describeFenceGateLeafMotion(facing, true, progress), AnimatedModelRenderContext.FenceGateQuadMode.LOW_LEAF_ONLY);
        drawFenceGateLeaf(world, renderer, consumers, matrices, cameraPosition, pos, closedState, HingeKinematics.describeFenceGateLeafMotion(facing, false, progress), AnimatedModelRenderContext.FenceGateQuadMode.HIGH_LEAF_ONLY);
    }

    private static void drawFenceGateLeaf(ClientWorld world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos pos, BlockState closedState, HingeKinematics.FenceGateLeafMotion motion, AnimatedModelRenderContext.FenceGateQuadMode mode) {
        AnimatedModelRenderContext.FenceGateQuadMode previousMode = AnimatedModelRenderContext.pushFenceGateMode(mode);
        try {
            matrices.push();

            Vec3d translated = new Vec3d(pos.getX() - cameraPosition.x, pos.getY() - cameraPosition.y, pos.getZ() - cameraPosition.z);
            Vec3d bias = biasTowardCamera(cameraPosition, pos);
            matrices.translate(translated.x + bias.x, translated.y, translated.z + bias.z);
            matrices.translate(motion.pivotX(), 0.0, motion.pivotZ());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(motion.degrees()));
            matrices.translate(-motion.pivotX(), 0.0, -motion.pivotZ());

            try {
                renderer.renderBlockAsEntity(closedState, matrices, consumers, WorldRenderer.getLightmapCoordinates(world, pos), OverlayTexture.DEFAULT_UV);
            } catch (RuntimeException ignored) {
            }

            matrices.pop();
        } finally {
            AnimatedModelRenderContext.restoreFenceGateMode(previousMode);
        }
    }

    private static void drawSingleLeaf(BlockRenderView world, BlockRenderManager renderer, VertexConsumerProvider consumers, MatrixStack matrices, Vec3d cameraPosition, BlockPos pos, BlockState closedState, float angleDegrees, DoorAnimationTimeline.MotionSlice slice) {
        matrices.push();

        Vec3d translated = new Vec3d(pos.getX() - cameraPosition.x, pos.getY() - cameraPosition.y, pos.getZ() - cameraPosition.z);
        Vec3d bias = biasTowardCamera(cameraPosition, pos);
        matrices.translate(translated.x + bias.x, translated.y, translated.z + bias.z);

        Direction facing = closedState.get(Properties.HORIZONTAL_FACING);
        DoorHinge hinge = closedState.get(Properties.DOOR_HINGE);
        HingeKinematics.Offset inset = HingeKinematics.computeHingeInset(facing, hinge, angleDegrees);
        matrices.translate(inset.x(), 0.0, inset.z());

        HingeKinematics.Anchor anchor = HingeKinematics.locateHingeAnchor(facing, hinge);
        matrices.translate(anchor.x(), 0.0, anchor.z());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angleDegrees));
        matrices.translate(-anchor.x(), 0.0, -anchor.z());

        try {
            renderer.renderBlockAsEntity(closedState, matrices, consumers, WorldRenderer.getLightmapCoordinates(world, pos), OverlayTexture.DEFAULT_UV);
        } catch (RuntimeException ignored) {
        }

        matrices.pop();
    }

    private static Vec3d biasTowardCamera(Vec3d cameraPosition, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double deltaX = cameraPosition.x - centerX;
        double deltaZ = cameraPosition.z - centerZ;
        double magnitude = Math.hypot(deltaX, deltaZ);

        if (magnitude <= 1.0E-6) {
            return Vec3d.ZERO;
        }

        double scale = CAMERA_BIAS / magnitude;
        return new Vec3d(deltaX * scale, 0.0, deltaZ * scale);
    }
}
