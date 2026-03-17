package io.github.derk.freshinteractiableanimations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
public final class DoorAnimationTimeline {
    private static final Logger LOGGER = LoggerFactory.getLogger("fresh-interactiable-animations");
    public static final long TRANSITION_LENGTH_NANOS = 240000000L;
    public static final long REVEAL_LEAD_TIME_NANOS = 50000000L;
    private static final long DUPLICATE_EVENT_WINDOW_NANOS = 150000000L;
    private static final Map<BlockPos, MotionSlice> ACTIVE_SLICES = new ConcurrentHashMap<>();

    private DoorAnimationTimeline() {
    }

    public static void recordTransition(AnimatedBlockSnapshot snapshot, boolean opening) {
        long now = System.nanoTime();
        BlockPos anchorPos = snapshot.anchorPos();
        MotionSlice prior = ACTIVE_SLICES.get(anchorPos);
        if (isDuplicateEvent(prior, snapshot, opening, now)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[FIA] suppressed duplicate transition kind={} state={} at {}", snapshot.kind(), opening ? "OPEN" : "CLOSE", anchorPos);
            }
            return;
        }

        MotionSlice next = new MotionSlice(snapshot.kind(), opening, now, snapshot.closedState());
        ACTIVE_SLICES.put(anchorPos, next);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[FIA] tracked transition kind={} state={} pos={} active={}", snapshot.kind(), opening, anchorPos, ACTIVE_SLICES.size());
        }
    }

    public static void flushCompleted() {
        long now = System.nanoTime();
        ACTIVE_SLICES.entrySet().removeIf(entry -> now - entry.getValue().startedAtNanos >= TRANSITION_LENGTH_NANOS);
    }

    public static void reset() {
        ACTIVE_SLICES.clear();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[FIA] timeline reset");
        }
    }

    public static Float sampleProgress(BlockPos anchorPos) {
        MotionSlice slice = ACTIVE_SLICES.get(anchorPos);
        if (slice == null) {
            return null;
        }

        long now = System.nanoTime();
        if (!slice.revealed && now >= slice.hiddenUntilNanos) {
            slice.revealed = true;
            refreshGeometry(anchorPos, slice);
        }
        if (now - slice.startedAtNanos >= TRANSITION_LENGTH_NANOS) {
            ACTIVE_SLICES.remove(anchorPos);
            refreshGeometry(anchorPos, slice);
            return null;
        }

        return HingeKinematics.sampleOpenProgress(slice.opening, slice.startedAtNanos, now, TRANSITION_LENGTH_NANOS);
    }

    public static void visitRunningTransitions(BiConsumer<BlockPos, MotionSlice> visitor) {
        ACTIVE_SLICES.forEach((pos, slice) -> visitor.accept(pos.toImmutable(), slice));
    }

    public static boolean shouldSuppressAnimatedModel(BlockPos pos, BlockState state) {
        long now = System.nanoTime();
        MotionSlice exact = ACTIVE_SLICES.get(pos);
        if (exact != null && now < exact.hiddenUntilNanos) {
            return true;
        }

        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            MotionSlice lowerHalf = ACTIVE_SLICES.get(pos.down());
            return lowerHalf != null && now < lowerHalf.hiddenUntilNanos;
        }

        return false;
    }

    public static boolean containsAnimatedBlock(BlockPos pos) {
        return ACTIVE_SLICES.containsKey(pos) || ACTIVE_SLICES.containsKey(pos.down());
    }

    private static boolean isDuplicateEvent(MotionSlice prior, AnimatedBlockSnapshot snapshot, boolean opening, long now) {
        return prior != null
            && prior.kind == snapshot.kind()
            && prior.opening == opening
            && prior.closedState.equals(snapshot.closedState())
            && now - prior.startedAtNanos < DUPLICATE_EVENT_WINDOW_NANOS;
    }

    private static void refreshGeometry(BlockPos anchorPos, MotionSlice slice) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) {
            return;
        }

        refreshCell(client, anchorPos);
        if (slice.kind == AnimatedBlockSnapshot.Kind.DOOR) {
            refreshCell(client, anchorPos.up());
        }
    }

    private static void refreshCell(MinecraftClient client, BlockPos pos) {
        try {
            BlockState currentState = client.world.getBlockState(pos);
            if (currentState == null) {
                return;
            }

            if (currentState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                BlockState swapped = currentState.cycle(Properties.DOUBLE_BLOCK_HALF);
                client.worldRenderer.updateBlock(null, pos, currentState, swapped, 0);
                client.worldRenderer.updateBlock(null, pos, swapped, currentState, 0);
                return;
            }

            if (currentState.contains(Properties.DOOR_HINGE)) {
                BlockState swapped = currentState.cycle(Properties.DOOR_HINGE);
                client.worldRenderer.updateBlock(null, pos, currentState, swapped, 0);
                client.worldRenderer.updateBlock(null, pos, swapped, currentState, 0);
                return;
            }

            client.worldRenderer.updateBlock(null, pos, currentState, currentState, 0);
        } catch (Throwable throwable) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("[FIA] refresh failed at {}: {}", pos, throwable.toString());
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static final class MotionSlice {
        public final AnimatedBlockSnapshot.Kind kind;
        public final boolean opening;
        public final long startedAtNanos;
        public final BlockState closedState;
        public final long hiddenUntilNanos;
        public boolean revealed;

        public MotionSlice(AnimatedBlockSnapshot.Kind kind, boolean opening, long startedAtNanos, BlockState closedState) {
            this.kind = kind;
            this.opening = opening;
            this.startedAtNanos = startedAtNanos;
            this.closedState = closedState;

            long finishAt = startedAtNanos + TRANSITION_LENGTH_NANOS;
            long revealLead = kind == AnimatedBlockSnapshot.Kind.FENCE_GATE
                ? 0L
                : Math.min(REVEAL_LEAD_TIME_NANOS, Math.max(0L, TRANSITION_LENGTH_NANOS - 1000000L));
            this.hiddenUntilNanos = Math.max(startedAtNanos, finishAt - revealLead);
            this.revealed = false;
        }
    }
}
