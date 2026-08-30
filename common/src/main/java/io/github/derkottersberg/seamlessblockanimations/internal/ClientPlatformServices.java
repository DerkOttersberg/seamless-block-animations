package io.github.derkottersberg.seamlessblockanimations.internal;

public interface ClientPlatformServices {
    String loaderName();

    void registerEndClientTick(Runnable callback);

    void registerSessionReset(Runnable callback);
}
