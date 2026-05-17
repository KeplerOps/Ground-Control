package com.keplerops.groundcontrol.domain.assets.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.assets.state.AssetSubtypeSchemaStatus;
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
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Per-project subtype schema entry for GC-M011 schema layering.
 *
 * <p>One row per {@code (project, assetType, subtype, schemaVersion)} tuple.
 * Exactly one row per {@code (project, assetType, subtype)} may be ACTIVE at a
 * time — enforced by {@code AssetService.registerSubtypeSchema} which auto-
 * deprecates the prior ACTIVE entry on registration.
 */
@Entity
@Audited
@Table(
        name = "asset_subtype_schema",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_asset_subtype_schema_version",
                        columnNames = {"project_id", "asset_type", "subtype", "schema_version"}))
public class AssetSubtypeSchema extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 100)
    private String subtype;

    @Column(name = "schema_version", nullable = false, length = 50)
    private String schemaVersion;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(name = "schema_body", columnDefinition = "TEXT")
    private Map<String, Object> schemaBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetSubtypeSchemaStatus status = AssetSubtypeSchemaStatus.ACTIVE;

    protected AssetSubtypeSchema() {
        // JPA
    }

    public AssetSubtypeSchema(
            Project project,
            AssetType assetType,
            String subtype,
            String schemaVersion,
            Map<String, Object> schemaBody) {
        this.project = project;
        this.assetType = assetType;
        this.subtype = subtype;
        this.schemaVersion = schemaVersion;
        this.schemaBody = schemaBody;
    }

    public Project getProject() {
        return project;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getSchemaBody() {
        return schemaBody;
    }

    public void setSchemaBody(Map<String, Object> schemaBody) {
        this.schemaBody = schemaBody;
    }

    public AssetSubtypeSchemaStatus getStatus() {
        return status;
    }

    public void setStatus(AssetSubtypeSchemaStatus status) {
        this.status = status;
    }

    public void deprecate() {
        this.status = AssetSubtypeSchemaStatus.DEPRECATED;
    }
}
