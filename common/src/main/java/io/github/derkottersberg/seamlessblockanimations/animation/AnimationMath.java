package io.github.derkottersberg.seamlessblockanimations.animation;

public final class AnimationMath {
    private static final long MIN_DURATION_NANOS = 1_000_000L;

    private AnimationMath() {
    }

    public static float cubicOut(float progress) {
        float clamped = Math.max(0.0f, Math.min(1.0f, progress));
        return clamped * (3.0f + clamped * (clamped - 3.0f));
    }

    public static float sample(
        float startProgress,
        float targetProgress,
        long startedAtNanos,
        long sampledAtNanos,
        long durationNanos
    ) {
        if (durationNanos <= 0L || sampledAtNanos >= startedAtNanos + durationNanos) {
            return targetProgress;
        }
        if (sampledAtNanos <= startedAtNanos) {
            return startProgress;
        }

        float elapsed = (float) (sampledAtNanos - startedAtNanos) / (float) durationNanos;
        float eased = cubicOut(elapsed);
        return startProgress + (targetProgress - startProgress) * eased;
    }

    public static long durationForDistance(long fullDurationNanos, float startProgress, float targetProgress) {
        float distance = Math.abs(targetProgress - startProgress);
        if (distance <= 0.0f) {
            return 0L;
        }
        return Math.max(MIN_DURATION_NANOS, Math.round(fullDurationNanos * Math.min(1.0f, distance)));
    }
}
