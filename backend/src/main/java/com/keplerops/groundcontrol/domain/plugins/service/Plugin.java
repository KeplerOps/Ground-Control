package com.keplerops.groundcontrol.domain.plugins.service;

public interface Plugin {

    PluginDescriptor descriptor();

    default void initialize() {
        // No-op by default. Override for plugins that need initialization.
    }

    default void start() {
        // No-op by default. Override for plugins that need startup logic.
    }

    default void stop() {
        // No-op by default. Override for plugins that need shutdown logic.
    }

    default boolean isAvailable() {
        return true;
    }
}
