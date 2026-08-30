package io.github.derkottersberg.seamlessblockanimations.fabric.gametest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.derkottersberg.seamlessblockanimations.animation.AnimationTimeline;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@SuppressWarnings("UnstableApiUsage")
public final class AnimationClientGameTest implements FabricClientGameTest {
    private static final BlockPos DOOR = new BlockPos(-2, 100, 0);
    private static final BlockPos TRAPDOOR = new BlockPos(0, 100, 0);
    private static final BlockPos FENCE_GATE = new BlockPos(2, 100, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        assertCombinedSuiteLoaded();
        assertRequestedGraphicsBackend();
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            arrangeWorld(singleplayer);
            singleplayer.getConnection().waitForChunksRender();
            context.waitFor(client -> client.level != null
                && client.level.getBlockState(DOOR).hasProperty(BlockStateProperties.OPEN));
            singleplayer.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            context.getInput().pressKey(InputConstants.KEY_F1);
            context.takeScreenshot("seamless-block-animations-closed");

            setOpen(singleplayer, true);
            waitForOpenState(context, true);
            assertRunningTransitions(context, 3);
            context.waitTick();
            context.takeScreenshot("seamless-block-animations-opening");

            setOpen(singleplayer, false);
            waitForOpenState(context, false);
            assertRunningTransitions(context, 3);
            context.waitTick();
            context.takeScreenshot("seamless-block-animations-reversing");

            context.waitTicks(8);
            assertRunningTransitions(context, 0);
            context.takeScreenshot("seamless-block-animations-closed-after-reversal");
        }
    }

    private static void assertRequestedGraphicsBackend() {
        if (!Boolean.getBoolean("seamless.vulkanTest")) {
            return;
        }

        String backend = RenderSystem.getDevice().getDeviceInfo().backendName();
        if (!backend.toLowerCase(Locale.ROOT).contains("vulkan")) {
            throw new AssertionError("Vulkan was requested, but Minecraft selected: " + backend);
        }
    }

    private static void assertCombinedSuiteLoaded() {
        if (!Boolean.getBoolean("seamless.suiteTest")) {
            return;
        }

        List<String> missing = List.of(
            "seamlessapi",
            "prettymeteors",
            "seamlessdeconstructor",
            "seamless_crafting",
            "seamless_block_animations"
        ).stream().filter(id -> !FabricLoader.getInstance().isModLoaded(id)).toList();
        if (!missing.isEmpty()) {
            throw new AssertionError("Combined suite is missing Fabric mods: " + missing);
        }
    }

    private static void arrangeWorld(TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("/time set noon");
        singleplayer.getServer().runCommand("/weather clear");
        singleplayer.getServer().runCommand("/fill -6 99 -3 6 99 3 minecraft:stone");
        singleplayer.getServer().runCommand("/gamemode spectator @a");
        singleplayer.getServer().runCommand("/tp @a 0.5 101 -7 facing 0.5 100.75 0.5");
        singleplayer.getServer().runCommand(
            "/setblock -2 100 0 minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock -2 101 0 minecraft:oak_door[facing=north,half=upper,hinge=left,open=false,powered=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock 0 100 0 minecraft:oak_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock 2 100 0 minecraft:oak_fence_gate[facing=north,in_wall=false,open=false,powered=false]"
        );
    }

    private static void setOpen(TestSingleplayerContext singleplayer, boolean open) {
        String value = Boolean.toString(open);
        singleplayer.getServer().runCommand(
            "/setblock -2 100 0 minecraft:oak_door[facing=north,half=lower,hinge=left,open=" + value + ",powered=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock -2 101 0 minecraft:oak_door[facing=north,half=upper,hinge=left,open=" + value + ",powered=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock 0 100 0 minecraft:oak_trapdoor[facing=north,half=bottom,open=" + value
                + ",powered=false,waterlogged=false]"
        );
        singleplayer.getServer().runCommand(
            "/setblock 2 100 0 minecraft:oak_fence_gate[facing=north,in_wall=false,open=" + value
                + ",powered=false]"
        );
    }

    private static void waitForOpenState(ClientGameTestContext context, boolean expected) {
        context.waitFor(client -> client.level != null
            && client.level.getBlockState(DOOR).getValue(BlockStateProperties.OPEN) == expected
            && client.level.getBlockState(TRAPDOOR).getValue(BlockStateProperties.OPEN) == expected
            && client.level.getBlockState(FENCE_GATE).getValue(BlockStateProperties.OPEN) == expected);
    }

    private static void assertRunningTransitions(ClientGameTestContext context, int expected) {
        int actual = context.computeOnClient(client -> {
            AtomicInteger count = new AtomicInteger();
            AnimationTimeline.visitRunningTransitions((pos, slice) -> count.incrementAndGet());
            return count.get();
        });
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " running transitions, got " + actual);
        }
    }
}
