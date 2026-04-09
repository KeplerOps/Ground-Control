package com.keplerops.groundcontrol.domain.plugins.service;

import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.Map;
import java.util.Set;

public record PluginDescriptor(
        String name,
        String version,
        String description,
        PluginType type,
        Set<String> capabilities,
        Map<String, Object> metadata) {}
