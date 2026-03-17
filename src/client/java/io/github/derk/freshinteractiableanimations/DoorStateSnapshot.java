package io.github.derk.freshinteractiableanimations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public record DoorStateSnapshot(BlockPos basePos, DoorHinge hinge, DoubleBlockHalf half, Direction facing) {
    public static DoorStateSnapshot from(BlockPos pos, BlockState state) {
        DoubleBlockHalf half = state.contains(Properties.DOUBLE_BLOCK_HALF) ? state.get(Properties.DOUBLE_BLOCK_HALF) : DoubleBlockHalf.LOWER;
        DoorHinge hinge = state.contains(Properties.DOOR_HINGE) ? state.get(Properties.DOOR_HINGE) : DoorHinge.LEFT;
        Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : Direction.NORTH;
        BlockPos basePos = half == DoubleBlockHalf.UPPER ? pos.down() : pos;
        return new DoorStateSnapshot(basePos, hinge, half, facing);
    }
}
