package io.github.derk.freshinteractiableanimations.mixin;

import io.github.derk.freshinteractiableanimations.AnimatedBlockSnapshot;
import io.github.derk.freshinteractiableanimations.DoorAnimationTimeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public final class NetworkDoorStateMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("fresh-interactiable-animations");
    private static final Map<BlockPos, Boolean> LAST_OPEN_STATES = new ConcurrentHashMap<>();

    @Inject(method = "onBlockUpdate", at = @At("HEAD"))
    private void freshInteractiableAnimations$observeBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        ClientWorld world = currentWorld();
        if (world == null) {
            return;
        }

        BlockPos pos = packet.getPos();
        captureTransition(pos, packet.getState(), world.getBlockState(pos));
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"))
    private void freshInteractiableAnimations$observeChunkDelta(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        ClientWorld world = currentWorld();
        if (world == null) {
            return;
        }

        packet.visitUpdates((pos, state) -> captureTransition(pos, state, world.getBlockState(pos)));
    }

    private static void captureTransition(BlockPos pos, BlockState newState, BlockState oldState) {
        AnimatedBlockSnapshot snapshot = AnimatedBlockSnapshot.from(pos, newState);
        if (snapshot == null) {
            return;
        }

        boolean previousOpen = resolvePreviousOpenState(snapshot, oldState);
        boolean currentOpen = newState.get(Properties.OPEN);
        LAST_OPEN_STATES.put(snapshot.anchorPos(), currentOpen);
        if (previousOpen == currentOpen) {
            return;
        }

        DoorAnimationTimeline.recordTransition(snapshot, currentOpen);
        scheduleSectionRefresh(snapshot);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[FIA][NET] kind={} pos={} anchor={} wasOpen={} isOpen={}", snapshot.kind(), pos, snapshot.anchorPos(), previousOpen, currentOpen);
        }
    }

    private static boolean resolvePreviousOpenState(AnimatedBlockSnapshot snapshot, BlockState oldState) {
        Boolean cached = LAST_OPEN_STATES.get(snapshot.anchorPos());
        if (cached != null) {
            return cached;
        }
        return oldState.getBlock() == snapshot.closedState().getBlock() && oldState.contains(Properties.OPEN) && oldState.get(Properties.OPEN);
    }

    private static void scheduleSectionRefresh(AnimatedBlockSnapshot snapshot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            if (client.worldRenderer == null) {
                return;
            }

            BlockPos basePos = snapshot.anchorPos();
            BlockPos topPos = basePos.up();
            int chunkX = basePos.getX() >> 4;
            int chunkZ = basePos.getZ() >> 4;
            int baseSectionY = basePos.getY() >> 4;
            int topSectionY = topPos.getY() >> 4;

            client.worldRenderer.scheduleBlockRenders(chunkX, baseSectionY, chunkZ, chunkX, baseSectionY, chunkZ);
            if (snapshot.kind() == AnimatedBlockSnapshot.Kind.DOOR && topSectionY != baseSectionY) {
                client.worldRenderer.scheduleBlockRenders(chunkX, topSectionY, chunkZ, chunkX, topSectionY, chunkZ);
            }
        });
    }

    private static ClientWorld currentWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null ? client.world : null;
    }
}
