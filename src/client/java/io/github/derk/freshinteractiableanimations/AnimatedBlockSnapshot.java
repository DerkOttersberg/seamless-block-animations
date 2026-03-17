package io.github.derk.freshinteractiableanimations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record AnimatedBlockSnapshot(Kind kind, BlockPos anchorPos, BlockState closedState) {
    public enum Kind {
        DOOR,
        TRAPDOOR,
        FENCE_GATE
    }

    @Nullable
    public static AnimatedBlockSnapshot from(BlockPos pos, BlockState state) {
        if (!state.contains(Properties.OPEN)) {
            return null;
        }

        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.contains(Properties.DOUBLE_BLOCK_HALF) ? state.get(Properties.DOUBLE_BLOCK_HALF) : DoubleBlockHalf.LOWER;
            BlockPos anchorPos = half == DoubleBlockHalf.UPPER ? pos.down() : pos;
            BlockState closedState = state.with(Properties.OPEN, false).with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            return new AnimatedBlockSnapshot(Kind.DOOR, anchorPos.toImmutable(), closedState);
        }

        if (state.getBlock() instanceof TrapdoorBlock) {
            return new AnimatedBlockSnapshot(Kind.TRAPDOOR, pos.toImmutable(), state.with(Properties.OPEN, false));
        }

        if (state.getBlock() instanceof FenceGateBlock) {
            return new AnimatedBlockSnapshot(Kind.FENCE_GATE, pos.toImmutable(), state.with(Properties.OPEN, false));
        }

        return null;
    }

    public static boolean supports(BlockState state) {
        return state.contains(Properties.OPEN)
            && (state.getBlock() instanceof DoorBlock
            || state.getBlock() instanceof TrapdoorBlock
            || state.getBlock() instanceof FenceGateBlock);
    }
}