package com.keplerops.groundcontrol.api.plugins;

import com.keplerops.groundcontrol.domain.plugins.service.PluginInfo;
import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.Map;
import java.util.Set;

public record PluginResponse(
        String name,
        String version,
        String description,
        PluginType type,
        Set<String> capabilities,
        Map<String, Object> metadata,
        PluginLifecycleState state,
        boolean available,
        boolean builtin) {

    public static PluginResponse from(PluginInfo info) {
        return new PluginResponse(
                info.name(),
                info.version(),
                info.description(),
                info.type(),
                info.capabilities(),
                info.metadata(),
                info.state(),
                info.available(),
                info.builtin());
    }
}
