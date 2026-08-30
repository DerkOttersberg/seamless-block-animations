package io.github.derkottersberg.seamlessblockanimations.forge;

import io.github.derkottersberg.seamlessblockanimations.SeamlessBlockAnimations;
import io.github.derkottersberg.seamlessblockanimations.internal.ClientPlatformServices;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SeamlessBlockAnimations.MOD_ID)
public final class SeamlessBlockAnimationsForge {
    public SeamlessBlockAnimationsForge() {
        if (FMLEnvironment.dist.isClient()) {
            SeamlessBlockAnimations.initializeClient(new ForgeClientPlatformServices());
        }
    }

    private static final class ForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public void registerEndClientTick(Runnable callback) {
            TickEvent.ClientTickEvent.Post.BUS.addListener(event -> callback.run());
        }

        @Override
        public void registerSessionReset(Runnable callback) {
            ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> callback.run());
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> callback.run());
        }
    }
}
