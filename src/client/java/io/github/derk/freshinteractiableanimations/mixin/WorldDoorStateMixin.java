package io.github.derk.freshinteractiableanimations.mixin;

import io.github.derk.freshinteractiableanimations.AnimatedBlockSnapshot;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import io.github.derk.freshinteractiableanimations.HingeKinematics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.class)
public final class WorldDoorStateMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("fresh-interactiable-animations");

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("HEAD"))
    private void freshInteractiableAnimations$observeLocalDoorState(BlockPos pos, BlockState newState, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        AnimatedBlockSnapshot snapshot = AnimatedBlockSnapshot.from(pos, newState);
        if (snapshot == null) {
            return;
        }

        ClientWorld world = (ClientWorld) (Object) this;
        BlockState oldState = world.getBlockState(pos);
        boolean previousOpen = oldState.getBlock() == newState.getBlock() && oldState.contains(Properties.OPEN) && oldState.get(Properties.OPEN);
        boolean currentOpen = newState.get(Properties.OPEN);
        if (previousOpen == currentOpen) {
            return;
        }

        if (snapshot.anchorPos().equals(pos)) {
            DoorAnimationTimeline.recordTransition(snapshot, currentOpen);
            markDoorForImmediateRefresh(snapshot);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[FIA] local transition kind={} pos={} open={} progressTarget={}", snapshot.kind(), snapshot.anchorPos(), currentOpen, currentOpen ? 1.0f : 0.0f);
            }
        }
    }

    private static void markDoorForImmediateRefresh(AnimatedBlockSnapshot snapshot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) {
            return;
        }

        BlockPos anchorPos = snapshot.anchorPos();
        BlockState anchorState = client.world.getBlockState(anchorPos);
        client.worldRenderer.updateBlock(null, anchorPos, anchorState, anchorState, 0);

        if (snapshot.kind() == AnimatedBlockSnapshot.Kind.DOOR) {
            BlockPos upperPos = anchorPos.up();
            BlockState upperState = client.world.getBlockState(upperPos);
            client.worldRenderer.updateBlock(null, upperPos, upperState, upperState, 0);
        }
    }
}
