package io.github.derk.freshinteractiableanimations;

import io.github.derk.freshinteractiableanimations.model.ConcealedDoorModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class FreshInteractiableAnimationsEntrypoint implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("fresh-interactiable-animations");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Fresh interactiable animations client initializing...");
        installLifecycleHooks();
        installDoorModelReplacement();
    }

    private static void installLifecycleHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                DoorAnimationTimeline.flushCompleted();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DoorAnimationTimeline.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DoorAnimationTimeline.reset());
    }

    private static void installDoorModelReplacement() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, view) -> {
            if (!AnimatedBlockSnapshot.supports(view.state())) {
                return model;
            }
            return new ConcealedDoorModel(model);
        }));
    }
}
