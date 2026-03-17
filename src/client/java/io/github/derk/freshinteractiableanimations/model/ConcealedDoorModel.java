package io.github.derk.freshinteractiableanimations.model;

import io.github.derk.freshinteractiableanimations.AnimatedBlockSnapshot;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;
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
        if (AnimatedBlockSnapshot.supports(state) && DoorAnimationTimeline.shouldSuppressAnimatedModel(pos, state)) {
            return;
        }

        super.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }
}
