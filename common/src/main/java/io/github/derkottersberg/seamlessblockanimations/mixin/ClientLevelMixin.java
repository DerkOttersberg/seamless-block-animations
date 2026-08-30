package io.github.derkottersberg.seamlessblockanimations.mixin;

import io.github.derkottersberg.seamlessblockanimations.animation.AnimatedBlockSnapshot;
import io.github.derkottersberg.seamlessblockanimations.animation.AnimationTimeline;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setBlock", at = @At("HEAD"))
    private void seamlessBlockAnimations$observeStateChange(
        BlockPos pos,
        BlockState newState,
        int flags,
        int recursionLeft,
        CallbackInfoReturnable<Boolean> cir
    ) {
        seamlessBlockAnimations$observe(pos, newState);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void seamlessBlockAnimations$observeServerStateChange(
        BlockPos pos,
        BlockState newState,
        int flags,
        CallbackInfo ci
    ) {
        seamlessBlockAnimations$observe(pos, newState);
    }

    @Unique
    private void seamlessBlockAnimations$observe(BlockPos pos, BlockState newState) {
        AnimatedBlockSnapshot snapshot = AnimatedBlockSnapshot.from(pos, newState);
        if (snapshot == null) {
            return;
        }

        ClientLevel level = (ClientLevel) (Object) this;
        BlockState oldState = level.getBlockState(pos);
        if (oldState.getBlock() != newState.getBlock()
            || !oldState.hasProperty(BlockStateProperties.OPEN)) {
            return;
        }

        boolean wasOpen = oldState.getValue(BlockStateProperties.OPEN);
        boolean isOpen = newState.getValue(BlockStateProperties.OPEN);
        if (wasOpen != isOpen) {
            AnimationTimeline.recordTransition(snapshot, isOpen);
        }
    }
}
