package io.github.derkottersberg.seamlessblockanimations.animation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

public record AnimatedBlockSnapshot(Kind kind, BlockPos anchorPos, BlockState closedState) {
    public enum Kind {
        DOOR,
        TRAPDOOR,
        FENCE_GATE
    }

    @Nullable
    public static AnimatedBlockSnapshot from(BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return null;
        }

        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                ? state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
                : DoubleBlockHalf.LOWER;
            BlockPos anchorPos = half == DoubleBlockHalf.UPPER ? pos.below() : pos;
            BlockState closedState = state
                .setValue(BlockStateProperties.OPEN, false)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            return new AnimatedBlockSnapshot(Kind.DOOR, anchorPos.immutable(), closedState);
        }

        if (state.getBlock() instanceof TrapDoorBlock) {
            return new AnimatedBlockSnapshot(
                Kind.TRAPDOOR,
                pos.immutable(),
                state.setValue(BlockStateProperties.OPEN, false)
            );
        }

        if (state.getBlock() instanceof FenceGateBlock) {
            return new AnimatedBlockSnapshot(
                Kind.FENCE_GATE,
                pos.immutable(),
                state.setValue(BlockStateProperties.OPEN, false)
            );
        }

        return null;
    }

    public static boolean supports(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN)
            && (state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock);
    }
}
