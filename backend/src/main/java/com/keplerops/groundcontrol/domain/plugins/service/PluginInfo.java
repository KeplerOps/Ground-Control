package com.keplerops.groundcontrol.domain.plugins.service;

import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.Map;
import java.util.Set;

public record PluginInfo(
        String name,
        String version,
        String description,
        PluginType type,
        Set<String> capabilities,
        Map<String, Object> metadata,
        PluginLifecycleState state,
        boolean available,
        boolean builtin) {}
