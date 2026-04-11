package com.keplerops.groundcontrol.domain.plugins.service;

import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record RegisterPluginCommand(
        UUID projectId,
        String name,
        String version,
        String description,
        PluginType type,
        Set<String> capabilities,
        Map<String, Object> metadata) {}
