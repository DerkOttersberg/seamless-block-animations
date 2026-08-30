package io.github.derkottersberg.seamlessblockanimations.animation;

import io.github.derkottersberg.seamlessblockanimations.SeamlessBlockAnimations;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

public final class AnimationTimeline {
    public static final long TRANSITION_LENGTH_NANOS = 240_000_000L;
    public static final long REVEAL_LEAD_TIME_NANOS = 50_000_000L;
    private static final float PROGRESS_EPSILON = 0.0001f;
    private static final Map<BlockPos, MotionSlice> ACTIVE = new ConcurrentHashMap<>();

    private AnimationTimeline() {
    }

    public static void recordTransition(AnimatedBlockSnapshot snapshot, boolean opening) {
        recordTransition(snapshot, opening, System.nanoTime());
    }

    static void recordTransition(AnimatedBlockSnapshot snapshot, boolean opening, long now) {
        BlockPos anchorPos = snapshot.anchorPos();
        float targetProgress = opening ? 1.0f : 0.0f;
        MotionSlice prior = ACTIVE.get(anchorPos);

        if (prior != null
            && prior.kind() == snapshot.kind()
            && Math.abs(prior.targetProgress() - targetProgress) <= PROGRESS_EPSILON
            && prior.closedState().equals(snapshot.closedState())) {
            return;
        }

        float startProgress = prior == null ? (opening ? 0.0f : 1.0f) : prior.progressAt(now);
        long duration = prior == null
            ? TRANSITION_LENGTH_NANOS
            : AnimationMath.durationForDistance(TRANSITION_LENGTH_NANOS, startProgress, targetProgress);

        if (duration == 0L || Math.abs(targetProgress - startProgress) <= PROGRESS_EPSILON) {
            ACTIVE.remove(anchorPos);
            refreshGeometry(anchorPos, snapshot.kind());
            return;
        }

        MotionSlice next = new MotionSlice(
            snapshot.kind(),
            startProgress,
            targetProgress,
            now,
            duration,
            snapshot.closedState()
        );
        ACTIVE.put(anchorPos, next);
        refreshGeometry(anchorPos, snapshot.kind());

        if (SeamlessBlockAnimations.LOGGER.isDebugEnabled()) {
            SeamlessBlockAnimations.LOGGER.debug(
                "Tracked {} transition at {} from {} to {} over {} ms",
                snapshot.kind(),
                anchorPos,
                startProgress,
                targetProgress,
                duration / 1_000_000L
            );
        }
    }

    public static void flushCompleted() {
        long now = System.nanoTime();
        ACTIVE.forEach((anchorPos, slice) -> {
            revealStaticModelIfReady(anchorPos, slice, now);
            if (slice.isComplete(now) && ACTIVE.remove(anchorPos, slice)) {
                refreshGeometry(anchorPos, slice.kind());
            }
        });
    }

    public static void reset() {
        Map<BlockPos, MotionSlice> previous = Map.copyOf(ACTIVE);
        ACTIVE.clear();
        previous.forEach((pos, slice) -> refreshGeometry(pos, slice.kind()));
    }

    @Nullable
    public static Float sampleProgress(BlockPos anchorPos) {
        long now = System.nanoTime();
        MotionSlice slice = ACTIVE.get(anchorPos);
        if (slice == null) {
            return null;
        }

        revealStaticModelIfReady(anchorPos, slice, now);
        if (slice.isComplete(now)) {
            if (ACTIVE.remove(anchorPos, slice)) {
                refreshGeometry(anchorPos, slice.kind());
            }
            return null;
        }
        return slice.progressAt(now);
    }

    public static void visitRunningTransitions(BiConsumer<BlockPos, MotionSlice> visitor) {
        Map.copyOf(ACTIVE).forEach((pos, slice) -> visitor.accept(pos.immutable(), slice));
    }

    public static boolean shouldSuppressStaticModel(BlockPos pos, BlockState state) {
        MotionSlice slice = findSlice(pos, state);
        return slice != null && System.nanoTime() < slice.hiddenUntilNanos();
    }

    @Nullable
    private static MotionSlice findSlice(BlockPos pos, BlockState state) {
        MotionSlice exact = ACTIVE.get(pos);
        if (exact != null) {
            return exact;
        }
        if (state.getBlock() instanceof DoorBlock
            && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return ACTIVE.get(pos.below());
        }
        return null;
    }

    private static void revealStaticModelIfReady(BlockPos anchorPos, MotionSlice slice, long now) {
        if (!slice.revealed() && now >= slice.hiddenUntilNanos()) {
            slice.markRevealed();
            refreshGeometry(anchorPos, slice.kind());
        }
    }

    private static void refreshGeometry(BlockPos anchorPos, AnimatedBlockSnapshot.Kind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.levelExtractor == null) {
            return;
        }

        markDirty(minecraft, anchorPos);
        if (kind == AnimatedBlockSnapshot.Kind.DOOR) {
            markDirty(minecraft, anchorPos.above());
        }
    }

    private static void markDirty(Minecraft minecraft, BlockPos pos) {
        minecraft.levelExtractor.setBlocksDirty(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX(), pos.getY(), pos.getZ()
        );
    }

    public static final class MotionSlice {
        private final AnimatedBlockSnapshot.Kind kind;
        private final float startProgress;
        private final float targetProgress;
        private final long startedAtNanos;
        private final long durationNanos;
        private final BlockState closedState;
        private final long hiddenUntilNanos;
        private volatile boolean revealed;

        MotionSlice(
            AnimatedBlockSnapshot.Kind kind,
            float startProgress,
            float targetProgress,
            long startedAtNanos,
            long durationNanos,
            BlockState closedState
        ) {
            this.kind = kind;
            this.startProgress = startProgress;
            this.targetProgress = targetProgress;
            this.startedAtNanos = startedAtNanos;
            this.durationNanos = durationNanos;
            this.closedState = closedState;

            long revealLead = kind == AnimatedBlockSnapshot.Kind.FENCE_GATE
                ? 0L
                : Math.min(REVEAL_LEAD_TIME_NANOS, Math.max(0L, durationNanos - 1_000_000L));
            this.hiddenUntilNanos = startedAtNanos + durationNanos - revealLead;
        }

        public AnimatedBlockSnapshot.Kind kind() {
            return kind;
        }

        public float startProgress() {
            return startProgress;
        }

        public float targetProgress() {
            return targetProgress;
        }

        public BlockState closedState() {
            return closedState;
        }

        public float progressAt(long now) {
            return AnimationMath.sample(startProgress, targetProgress, startedAtNanos, now, durationNanos);
        }

        public boolean isComplete(long now) {
            return now - startedAtNanos >= durationNanos;
        }

        long hiddenUntilNanos() {
            return hiddenUntilNanos;
        }

        boolean revealed() {
            return revealed;
        }

        void markRevealed() {
            revealed = true;
        }
    }
}
