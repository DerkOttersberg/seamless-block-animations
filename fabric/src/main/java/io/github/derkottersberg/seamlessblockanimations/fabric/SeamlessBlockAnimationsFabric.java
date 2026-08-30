package io.github.derkottersberg.seamlessblockanimations.fabric;

import io.github.derkottersberg.seamlessblockanimations.SeamlessBlockAnimations;
import io.github.derkottersberg.seamlessblockanimations.internal.ClientPlatformServices;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SeamlessBlockAnimationsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SeamlessBlockAnimations.initializeClient(new FabricClientPlatformServices());
    }

    private static final class FabricClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public void registerEndClientTick(Runnable callback) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
        }

        @Override
        public void registerSessionReset(Runnable callback) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> callback.run());
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> callback.run());
        }
    }
}
