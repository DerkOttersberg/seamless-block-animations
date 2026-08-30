package io.github.derkottersberg.seamlessblockanimations;

import io.github.derkottersberg.seamlessblockanimations.animation.AnimationTimeline;
import io.github.derkottersberg.seamlessblockanimations.internal.ClientPlatformServices;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SeamlessBlockAnimations {
    public static final String MOD_ID = "seamless_block_animations";
    public static final String LEGACY_FABRIC_ID = "fresh-interactiable-animations";
    public static final Logger LOGGER = LoggerFactory.getLogger("Seamless Block Animations");

    private static ClientPlatformServices platform;

    private SeamlessBlockAnimations() {
    }

    public static synchronized void initializeClient(ClientPlatformServices services) {
        Objects.requireNonNull(services, "services");
        if (platform != null) {
            if (platform != services) {
                throw new IllegalStateException("Seamless Block Animations was initialized more than once");
            }
            return;
        }

        platform = services;
        services.registerEndClientTick(AnimationTimeline::flushCompleted);
        services.registerSessionReset(AnimationTimeline::reset);
        LOGGER.info("Seamless Block Animations initialized on {}", services.loaderName());
    }
}
