package com.keplerops.groundcontrol.domain.assets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.envers.Audited;

/**
 * An external identifier that maps an operational asset to its representation
 * in an external source system (e.g. AWS ARN, Terraform resource ID, ServiceNow CI).
 * Each asset may have identifiers from multiple partially overlapping sources.
 */
@Entity
@Audited
@Table(
        name = "asset_external_id",
        uniqueConstraints = @UniqueConstraint(columnNames = {"asset_id", "source_system", "source_id"}))
public class AssetExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private OperationalAsset asset;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_id", nullable = false, length = 500)
    private String sourceId;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(length = 50)
    private String confidence;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AssetExternalId() {
        // JPA
    }

    public AssetExternalId(OperationalAsset asset, String sourceSystem, String sourceId) {
        this.asset = asset;
        this.sourceSystem = sourceSystem;
        this.sourceId = sourceId;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public OperationalAsset getAsset() {
        return asset;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
