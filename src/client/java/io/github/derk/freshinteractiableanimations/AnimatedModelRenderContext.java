package io.github.derk.freshinteractiableanimations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AnimatedModelRenderContext {
    private static final ThreadLocal<FenceGateQuadMode> FENCE_GATE_MODE = ThreadLocal.withInitial(() -> FenceGateQuadMode.DEFAULT);

    private AnimatedModelRenderContext() {
    }

    public static FenceGateQuadMode currentFenceGateMode() {
        return FENCE_GATE_MODE.get();
    }

    public static FenceGateQuadMode pushFenceGateMode(FenceGateQuadMode mode) {
        FenceGateQuadMode previous = FENCE_GATE_MODE.get();
        FENCE_GATE_MODE.set(mode);
        return previous;
    }

    public static void restoreFenceGateMode(FenceGateQuadMode previous) {
        FENCE_GATE_MODE.set(previous);
    }

    @Environment(EnvType.CLIENT)
    public enum FenceGateQuadMode {
        DEFAULT,
        POSTS_ONLY,
        LOW_LEAF_ONLY,
        HIGH_LEAF_ONLY
    }
}