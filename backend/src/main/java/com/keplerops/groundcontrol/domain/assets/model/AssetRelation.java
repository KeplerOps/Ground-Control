package com.keplerops.groundcontrol.domain.assets.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.envers.Audited;

/**
 * A directed, typed relationship between two operational assets.
 */
@Entity
@Audited
@Table(
        name = "asset_relation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "target_id", "relation_type"}))
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class AssetRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private OperationalAsset source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private OperationalAsset target;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private AssetRelationType relationType;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "external_source_id", length = 500)
    private String externalSourceId;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(length = 50)
    private String confidence;

    protected AssetRelation() {
        // JPA
    }

    /*@ requires source != null && target != null && relationType != null;
    @ requires !source.getId().equals(target.getId());
    @ ensures this.source == source;
    @ ensures this.target == target;
    @ ensures this.relationType == relationType; @*/
    public AssetRelation(OperationalAsset source, OperationalAsset target, AssetRelationType relationType) {
        if (source.getId() != null && source.getId().equals(target.getId())) {
            throw new DomainValidationException("An asset cannot relate to itself");
        }
        this.source = source;
        this.target = target;
        this.relationType = relationType;
    }

    public OperationalAsset getSource() {
        return source;
    }

    public OperationalAsset getTarget() {
        return target;
    }

    public AssetRelationType getRelationType() {
        return relationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public void setExternalSourceId(String externalSourceId) {
        this.externalSourceId = externalSourceId;
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

    @Override
    public String toString() {
        return source.getUid() + " --[" + relationType + "]--> " + target.getUid();
    }
}
