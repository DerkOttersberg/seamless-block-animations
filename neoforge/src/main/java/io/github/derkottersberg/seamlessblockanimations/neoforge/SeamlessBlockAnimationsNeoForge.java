package io.github.derkottersberg.seamlessblockanimations.neoforge;

import io.github.derkottersberg.seamlessblockanimations.SeamlessBlockAnimations;
import io.github.derkottersberg.seamlessblockanimations.internal.ClientPlatformServices;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SeamlessBlockAnimations.MOD_ID, dist = Dist.CLIENT)
public final class SeamlessBlockAnimationsNeoForge {
    public SeamlessBlockAnimationsNeoForge() {
        SeamlessBlockAnimations.initializeClient(new NeoForgeClientPlatformServices());
    }

    private static final class NeoForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public void registerEndClientTick(Runnable callback) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> callback.run());
        }

        @Override
        public void registerSessionReset(Runnable callback) {
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> callback.run());
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> callback.run());
        }
    }
}
