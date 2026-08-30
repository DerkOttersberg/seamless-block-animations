package io.github.derkottersberg.seamlessblockanimations.render;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class AnimatedModelRenderContext {
    private static final ThreadLocal<Deque<StaticModelContext>> STATIC_CONTEXTS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private AnimatedModelRenderContext() {
    }

    public static void pushStaticModel(BlockPos pos, BlockState state) {
        STATIC_CONTEXTS.get().push(new StaticModelContext(pos.immutable(), state));
    }

    public static void popStaticModel() {
        Deque<StaticModelContext> contexts = STATIC_CONTEXTS.get();
        if (!contexts.isEmpty()) {
            contexts.pop();
        }
        if (contexts.isEmpty()) {
            STATIC_CONTEXTS.remove();
        }
    }

    @Nullable
    public static StaticModelContext currentStaticModel() {
        return STATIC_CONTEXTS.get().peek();
    }

    public enum QuadMode {
        POSTS_ONLY,
        LOW_LEAF_ONLY,
        HIGH_LEAF_ONLY
    }

    public record StaticModelContext(BlockPos pos, BlockState state) {
    }
}
