package com.keplerops.groundcontrol.domain.plugins.repository;

import com.keplerops.groundcontrol.domain.plugins.model.RegisteredPlugin;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisteredPluginRepository extends JpaRepository<RegisteredPlugin, UUID> {

    List<RegisteredPlugin> findByProjectId(UUID projectId);

    List<RegisteredPlugin> findByProjectIdAndPluginType(UUID projectId, PluginType pluginType);

    Optional<RegisteredPlugin> findByProjectIdAndName(UUID projectId, String name);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
