package com.keplerops.groundcontrol.api.plugins;

import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;

public record RegisterPluginRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String version,
        String description,
        @NotNull PluginType type,
        Set<String> capabilities,
        Map<String, Object> metadata) {}
