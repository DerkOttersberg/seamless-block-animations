package io.github.derkottersberg.seamlessblockanimations.render;

import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class FilteredBlockStateModelPart implements BlockStateModelPart {
    private static final float OUTER_POST_BAND = 0.125f;
    private static final float POSITION_EPSILON = 0.0001f;

    private final BlockStateModelPart delegate;
    private final BlockState state;
    private final AnimatedModelRenderContext.QuadMode mode;

    public FilteredBlockStateModelPart(
        BlockStateModelPart delegate,
        BlockState state,
        AnimatedModelRenderContext.QuadMode mode
    ) {
        this.delegate = delegate;
        this.state = state;
        this.mode = mode;
    }

    @Override
    public List<BakedQuad> getQuads(Direction direction) {
        List<BakedQuad> quads = delegate.getQuads(direction);
        if (quads.isEmpty()) {
            return quads;
        }
        return quads.stream().filter(this::shouldKeep).toList();
    }

    private boolean shouldKeep(BakedQuad quad) {
        FenceGateQuadRegion region = classifyFenceGateQuad(quad, state);
        return switch (mode) {
            case POSTS_ONLY -> region == FenceGateQuadRegion.POSTS;
            case LOW_LEAF_ONLY -> region == FenceGateQuadRegion.LOW_LEAF;
            case HIGH_LEAF_ONLY -> region == FenceGateQuadRegion.HIGH_LEAF;
        };
    }

    private static FenceGateQuadRegion classifyFenceGateQuad(BakedQuad quad, BlockState state) {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int vertexIndex = 0; vertexIndex < BakedQuad.VERTEX_COUNT; vertexIndex++) {
            minX = Math.min(minX, quad.position(vertexIndex).x());
            maxX = Math.max(maxX, quad.position(vertexIndex).x());
            minZ = Math.min(minZ, quad.position(vertexIndex).z());
            maxZ = Math.max(maxZ, quad.position(vertexIndex).z());
        }

        boolean alongX = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis() == Direction.Axis.Z;
        float minSpan = alongX ? minX : minZ;
        float maxSpan = alongX ? maxX : maxZ;
        if (maxSpan <= OUTER_POST_BAND + POSITION_EPSILON
            || minSpan >= 1.0f - OUTER_POST_BAND - POSITION_EPSILON) {
            return FenceGateQuadRegion.POSTS;
        }

        float midpoint = (minSpan + maxSpan) * 0.5f;
        return midpoint < 0.5f ? FenceGateQuadRegion.LOW_LEAF : FenceGateQuadRegion.HIGH_LEAF;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return delegate.materialFlags();
    }

    private enum FenceGateQuadRegion {
        POSTS,
        LOW_LEAF,
        HIGH_LEAF
    }
}
