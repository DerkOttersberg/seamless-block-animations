package io.github.derkottersberg.seamlessblockanimations.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.derkottersberg.seamlessblockanimations.animation.AnimatedBlockSnapshot;
import io.github.derkottersberg.seamlessblockanimations.animation.AnimationTimeline;
import io.github.derkottersberg.seamlessblockanimations.animation.HingeKinematics;
import io.github.derkottersberg.seamlessblockanimations.mixin.BlockModelRenderStateAccessor;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

public final class AnimatedBlockRenderer {
    private static final double CAMERA_BIAS = 0.0005;
    private static final int NO_OUTLINE = 0;

    private AnimatedBlockRenderer() {
    }

    public static void submit(LevelRenderState renderState, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || renderState.cameraRenderState == null) {
            return;
        }

        Vec3 cameraPosition = renderState.cameraRenderState.pos;
        BlockModelResolver resolver = new BlockModelResolver(minecraft.getModelManager());
        AnimationTimeline.visitRunningTransitions((anchorPos, slice) ->
            drawAnimatedBlock(level, resolver, collector, cameraPosition, anchorPos, slice));
    }

    private static void drawAnimatedBlock(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos anchorPos,
        AnimationTimeline.MotionSlice slice
    ) {
        Float progress = AnimationTimeline.sampleProgress(anchorPos);
        if (progress == null) {
            return;
        }

        switch (slice.kind()) {
            case DOOR -> drawDoorPair(level, resolver, collector, cameraPosition, anchorPos, slice, progress);
            case TRAPDOOR -> drawTrapdoor(level, resolver, collector, cameraPosition, anchorPos, slice, progress);
            case FENCE_GATE -> drawFenceGate(level, resolver, collector, cameraPosition, anchorPos, slice, progress);
        }
    }

    private static void drawDoorPair(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos basePos,
        AnimationTimeline.MotionSlice slice,
        float progress
    ) {
        if (!(level.getBlockState(basePos).getBlock() instanceof DoorBlock)) {
            return;
        }

        BlockState lowerClosedState = slice.closedState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperClosedState = slice.closedState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        Direction facing = lowerClosedState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        DoorHingeSide hinge = lowerClosedState.getValue(BlockStateProperties.DOOR_HINGE);
        float angle = HingeKinematics.sampleDoorAngleDegrees(facing, hinge, progress);

        drawDoorLeaf(level, resolver, collector, cameraPosition, basePos, lowerClosedState, angle);
        drawDoorLeaf(level, resolver, collector, cameraPosition, basePos.above(), upperClosedState, angle);
    }

    private static void drawDoorLeaf(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos pos,
        BlockState closedState,
        float angleDegrees
    ) {
        PoseStack pose = blockPose(cameraPosition, pos);
        Direction facing = closedState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        DoorHingeSide hinge = closedState.getValue(BlockStateProperties.DOOR_HINGE);
        HingeKinematics.Offset inset = HingeKinematics.computeHingeInset(facing, hinge, angleDegrees);
        pose.translate(inset.x(), 0.0, inset.z());

        HingeKinematics.Anchor anchor = HingeKinematics.locateHingeAnchor(facing, hinge);
        pose.translate(anchor.x(), 0.0, anchor.z());
        pose.mulPose(Axis.YP.rotationDegrees(angleDegrees));
        pose.translate(-anchor.x(), 0.0, -anchor.z());
        submitModel(level, resolver, collector, pose, pos, closedState, null);
    }

    private static void drawTrapdoor(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos pos,
        AnimationTimeline.MotionSlice slice,
        float progress
    ) {
        if (!(level.getBlockState(pos).getBlock() instanceof TrapDoorBlock)) {
            return;
        }

        BlockState closedState = slice.closedState();
        Half half = closedState.getValue(BlockStateProperties.HALF);
        HingeKinematics.TrapdoorPose trapdoorPose = HingeKinematics.describeTrapdoorPose(
            closedState.getValue(BlockStateProperties.HORIZONTAL_FACING),
            half,
            progress
        );
        float closedCenterY = half == Half.TOP
            ? 1.0f - HingeKinematics.PANEL_THICKNESS * 0.5f
            : HingeKinematics.PANEL_THICKNESS * 0.5f;

        PoseStack pose = blockPose(cameraPosition, pos);
        pose.translate(trapdoorPose.centerX(), trapdoorPose.centerY(), trapdoorPose.centerZ());
        if (trapdoorPose.axis() == HingeKinematics.Axis.X) {
            pose.mulPose(Axis.XP.rotationDegrees(trapdoorPose.degrees()));
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(trapdoorPose.degrees()));
        }
        pose.translate(-0.5, -closedCenterY, -0.5);
        submitModel(level, resolver, collector, pose, pos, closedState, null);
    }

    private static void drawFenceGate(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos pos,
        AnimationTimeline.MotionSlice slice,
        float progress
    ) {
        if (!(level.getBlockState(pos).getBlock() instanceof FenceGateBlock)) {
            return;
        }

        BlockState closedState = slice.closedState();
        Direction facing = closedState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        drawFenceGateLeaf(
            level,
            resolver,
            collector,
            cameraPosition,
            pos,
            closedState,
            HingeKinematics.describeFenceGateLeafMotion(facing, true, progress),
            AnimatedModelRenderContext.QuadMode.LOW_LEAF_ONLY
        );
        drawFenceGateLeaf(
            level,
            resolver,
            collector,
            cameraPosition,
            pos,
            closedState,
            HingeKinematics.describeFenceGateLeafMotion(facing, false, progress),
            AnimatedModelRenderContext.QuadMode.HIGH_LEAF_ONLY
        );
    }

    private static void drawFenceGateLeaf(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        Vec3 cameraPosition,
        BlockPos pos,
        BlockState closedState,
        HingeKinematics.FenceGateLeafMotion motion,
        AnimatedModelRenderContext.QuadMode mode
    ) {
        PoseStack pose = blockPose(cameraPosition, pos);
        pose.translate(motion.pivotX(), 0.0, motion.pivotZ());
        pose.mulPose(Axis.YP.rotationDegrees(motion.degrees()));
        pose.translate(-motion.pivotX(), 0.0, -motion.pivotZ());
        submitModel(level, resolver, collector, pose, pos, closedState, mode);
    }

    private static PoseStack blockPose(Vec3 cameraPosition, BlockPos pos) {
        PoseStack pose = new PoseStack();
        Vec3 bias = biasTowardCamera(cameraPosition, pos);
        pose.translate(
            pos.getX() - cameraPosition.x + bias.x,
            pos.getY() - cameraPosition.y,
            pos.getZ() - cameraPosition.z + bias.z
        );
        return pose;
    }

    private static void submitModel(
        ClientLevel level,
        BlockModelResolver resolver,
        SubmitNodeCollector collector,
        PoseStack pose,
        BlockPos pos,
        BlockState state,
        AnimatedModelRenderContext.QuadMode mode
    ) {
        BlockModelRenderState modelRenderState = new BlockModelRenderState();
        resolver.update(modelRenderState, state, BlockDisplayContext.create());
        if (mode != null) {
            List<BlockStateModelPart> parts =
                ((BlockModelRenderStateAccessor) (Object) modelRenderState).seamlessBlockAnimations$getModelParts();
            if (parts != null) {
                parts.replaceAll(part -> new FilteredBlockStateModelPart(part, state, mode));
            }
        }
        modelRenderState.submit(
            pose,
            collector,
            LightCoordsUtil.getLightCoords(level, pos),
            OverlayTexture.NO_OVERLAY,
            NO_OUTLINE
        );
    }

    private static Vec3 biasTowardCamera(Vec3 cameraPosition, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double deltaX = cameraPosition.x - centerX;
        double deltaZ = cameraPosition.z - centerZ;
        double magnitude = Math.hypot(deltaX, deltaZ);
        if (magnitude <= 1.0E-6) {
            return Vec3.ZERO;
        }
        double scale = CAMERA_BIAS / magnitude;
        return new Vec3(deltaX * scale, 0.0, deltaZ * scale);
    }
}
