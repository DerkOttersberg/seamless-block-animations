package io.github.derkottersberg.seamlessblockanimations.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AnimationMathTest {
    @Test
    void cubicOutKeepsEndpointsAndExpectedMidpoint() {
        assertEquals(0.0f, AnimationMath.cubicOut(-1.0f));
        assertEquals(0.875f, AnimationMath.cubicOut(0.5f), 0.00001f);
        assertEquals(1.0f, AnimationMath.cubicOut(2.0f));
    }

    @Test
    void reverseTransitionStartsAtCurrentProgressWithoutSnapping() {
        long fullDuration = 240_000_000L;
        long reversalTime = 120_000_000L;
        float progressAtReversal = AnimationMath.sample(0.0f, 1.0f, 0L, reversalTime, fullDuration);
        long reverseDuration = AnimationMath.durationForDistance(fullDuration, progressAtReversal, 0.0f);

        assertEquals(0.875f, progressAtReversal, 0.00001f);
        assertEquals(progressAtReversal, AnimationMath.sample(
            progressAtReversal,
            0.0f,
            reversalTime,
            reversalTime,
            reverseDuration
        ));
        assertEquals(0.0f, AnimationMath.sample(
            progressAtReversal,
            0.0f,
            reversalTime,
            reversalTime + reverseDuration,
            reverseDuration
        ));
    }

    @Test
    void reversalDurationScalesWithRemainingAngularDistance() {
        assertEquals(240_000_000L, AnimationMath.durationForDistance(240_000_000L, 0.0f, 1.0f));
        assertEquals(60_000_000L, AnimationMath.durationForDistance(240_000_000L, 0.25f, 0.0f));
        assertEquals(0L, AnimationMath.durationForDistance(240_000_000L, 0.4f, 0.4f));
    }
}
