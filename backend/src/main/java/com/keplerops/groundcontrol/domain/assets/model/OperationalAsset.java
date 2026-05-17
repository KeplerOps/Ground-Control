package com.keplerops.groundcontrol.domain.assets.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "operational_asset", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class OperationalAsset extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType = AssetType.OTHER;

    @Column(length = 200)
    private String owner;

    @Column(length = 200)
    private String steward;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssetEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssetCriticality criticality;

    @Column(name = "business_context", columnDefinition = "TEXT")
    private String businessContext;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_designation", length = 20)
    private AssetScope scopeDesignation;

    /**
     * GC-M011 narrower classification under {@link AssetType}. Free-form,
     * project-defined; a registered {@code AssetSubtypeSchema} may formalize
     * the {@code (project, assetType, subtype)} contract.
     */
    @Column(length = 100)
    private String subtype;

    /**
     * GC-M011 subtype-specific metadata bag. Bounded by
     * {@code AssetSubtypeValidator}; schema-validated when a matching ACTIVE
     * {@code AssetSubtypeSchema} exists.
     */
    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected OperationalAsset() {
        // JPA
    }

    public OperationalAsset(Project project, String uid, String name) {
        this.project = project;
        this.uid = uid;
        this.name = name;
    }

    public void archive() {
        if (this.archivedAt == null) {
            this.archivedAt = Instant.now();
        }
    }

    public Project getProject() {
        return project;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSteward() {
        return steward;
    }

    public void setSteward(String steward) {
        this.steward = steward;
    }

    public AssetEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(AssetEnvironment environment) {
        this.environment = environment;
    }

    public AssetCriticality getCriticality() {
        return criticality;
    }

    public void setCriticality(AssetCriticality criticality) {
        this.criticality = criticality;
    }

    public String getBusinessContext() {
        return businessContext;
    }

    public void setBusinessContext(String businessContext) {
        this.businessContext = businessContext;
    }

    public AssetScope getScopeDesignation() {
        return scopeDesignation;
    }

    public void setScopeDesignation(AssetScope scopeDesignation) {
        this.scopeDesignation = scopeDesignation;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return uid + ": " + name;
    }
}
