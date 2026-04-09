package com.keplerops.groundcontrol.domain.plugins.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "registered_plugin")
public class RegisteredPlugin extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "plugin_type", nullable = false, length = 30)
    private PluginType pluginType;

    @Convert(converter = JacksonTextCollectionConverters.StringSetConverter.class)
    @Column(columnDefinition = "TEXT")
    private Set<String> capabilities;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 20)
    private PluginLifecycleState lifecycleState;

    @Column(nullable = false)
    private boolean enabled;

    protected RegisteredPlugin() {
        // JPA
    }

    public RegisteredPlugin(Project project, String name, String version, PluginType pluginType) {
        this.project = project;
        this.name = name;
        this.version = version;
        this.pluginType = pluginType;
        this.lifecycleState = PluginLifecycleState.CREATED;
        this.enabled = true;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
        this.pluginType = pluginType;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public PluginLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(PluginLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
