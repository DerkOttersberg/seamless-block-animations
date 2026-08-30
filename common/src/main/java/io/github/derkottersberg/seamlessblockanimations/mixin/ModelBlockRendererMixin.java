package io.github.derkottersberg.seamlessblockanimations.mixin;

import io.github.derkottersberg.seamlessblockanimations.animation.AnimatedBlockSnapshot;
import io.github.derkottersberg.seamlessblockanimations.animation.AnimationTimeline;
import io.github.derkottersberg.seamlessblockanimations.render.AnimatedModelRenderContext;
import io.github.derkottersberg.seamlessblockanimations.render.FilteredBlockStateModelPart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @Inject(method = "tesselateBlock", at = @At("HEAD"))
    private void seamlessBlockAnimations$pushStaticContext(
        BlockQuadOutput output,
        float offsetX,
        float offsetY,
        float offsetZ,
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        BlockStateModel model,
        long seed,
        CallbackInfo ci
    ) {
        AnimatedModelRenderContext.pushStaticModel(pos, state);
    }

    @Redirect(
        method = "tesselateBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;collectParts(Lnet/minecraft/util/RandomSource;Ljava/util/List;)V"
        )
    )
    private void seamlessBlockAnimations$filterCompiledModel(
        BlockStateModel model,
        RandomSource random,
        List<BlockStateModelPart> destination
    ) {
        AnimatedModelRenderContext.StaticModelContext context =
            AnimatedModelRenderContext.currentStaticModel();
        if (context == null
            || !AnimatedBlockSnapshot.supports(context.state())
            || !AnimationTimeline.shouldSuppressStaticModel(context.pos(), context.state())) {
            model.collectParts(random, destination);
            return;
        }

        if (!(context.state().getBlock() instanceof FenceGateBlock)) {
            return;
        }

        List<BlockStateModelPart> originalParts = new ArrayList<>();
        model.collectParts(random, originalParts);
        originalParts.stream()
            .map(part -> new FilteredBlockStateModelPart(
                part,
                context.state(),
                AnimatedModelRenderContext.QuadMode.POSTS_ONLY
            ))
            .forEach(destination::add);
    }

    @Inject(method = "tesselateBlock", at = @At("RETURN"))
    private void seamlessBlockAnimations$popStaticContext(
        BlockQuadOutput output,
        float offsetX,
        float offsetY,
        float offsetZ,
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        BlockStateModel model,
        long seed,
        CallbackInfo ci
    ) {
        AnimatedModelRenderContext.popStaticModel();
    }
}
