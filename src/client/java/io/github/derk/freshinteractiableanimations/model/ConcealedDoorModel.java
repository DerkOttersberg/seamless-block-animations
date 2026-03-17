package io.github.derk.freshinteractiableanimations.model;

import io.github.derk.freshinteractiableanimations.AnimatedBlockSnapshot;
import io.github.derk.freshinteractiableanimations.AnimatedModelRenderContext;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class ConcealedDoorModel extends WrapperBlockStateModel implements FabricBlockStateModel {
    public ConcealedDoorModel(BlockStateModel delegate) {
        super(delegate);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Random random, Predicate<@Nullable Direction> cullTest) {
        if (state.getBlock() instanceof FenceGateBlock) {
            AnimatedModelRenderContext.FenceGateQuadMode mode = AnimatedModelRenderContext.currentFenceGateMode();
            if (mode != AnimatedModelRenderContext.FenceGateQuadMode.DEFAULT) {
                emitFilteredFenceGateQuads(emitter, blockView, pos, state, random, cullTest, mode);
                return;
            }

            if (DoorAnimationTimeline.shouldSuppressAnimatedModel(pos, state)) {
                return;
            }
        }

        if (AnimatedBlockSnapshot.supports(state) && DoorAnimationTimeline.shouldSuppressAnimatedModel(pos, state)) {
            return;
        }

        super.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }

    private void emitFilteredFenceGateQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Random random, Predicate<@Nullable Direction> cullTest, AnimatedModelRenderContext.FenceGateQuadMode mode) {
        emitter.pushTransform(quad -> shouldKeepFenceGateQuad(quad, state, mode));
        try {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private static boolean shouldKeepFenceGateQuad(QuadView quad, BlockState state, AnimatedModelRenderContext.FenceGateQuadMode mode) {
        FenceGateQuadRegion region = classifyFenceGateQuad(quad, state);
        return switch (mode) {
            case LOW_LEAF_ONLY -> region == FenceGateQuadRegion.LOW_LEAF;
            case HIGH_LEAF_ONLY -> region == FenceGateQuadRegion.HIGH_LEAF;
            case DEFAULT -> true;
        };
    }

    private static FenceGateQuadRegion classifyFenceGateQuad(QuadView quad, BlockState state) {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            minX = Math.min(minX, quad.x(vertexIndex));
            maxX = Math.max(maxX, quad.x(vertexIndex));
            minZ = Math.min(minZ, quad.z(vertexIndex));
            maxZ = Math.max(maxZ, quad.z(vertexIndex));
        }

        boolean alongX = state.get(Properties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z;
        float minSpan = alongX ? minX : minZ;
        float maxSpan = alongX ? maxX : maxZ;
        float midpoint = (minSpan + maxSpan) * 0.5f;
        return midpoint < 0.5f ? FenceGateQuadRegion.LOW_LEAF : FenceGateQuadRegion.HIGH_LEAF;
    }

    private enum FenceGateQuadRegion {
        LOW_LEAF,
        HIGH_LEAF
    }
}
