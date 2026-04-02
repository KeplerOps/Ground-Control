package com.keplerops.groundcontrol.domain.assets.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
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
 * A time-bounded state fact observed about an operational asset, such as a
 * configuration value, exposure status, identity assignment, deployment
 * attribute, patch state, or discovered relationship. Observations remain
 * distinct from the asset definition and record source, observed-at time,
 * freshness/validity window, and supporting evidence references.
 */
@Entity
@Audited
@Table(
        name = "observation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"asset_id", "category", "observation_key", "observed_at"}))
public class Observation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private OperationalAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ObservationCategory category;

    @Column(name = "observation_key", nullable = false, length = 200)
    private String observationKey;

    @Column(name = "observation_value", nullable = false, columnDefinition = "TEXT")
    private String observationValue;

    @Column(nullable = false, length = 200)
    private String source;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 50)
    private String confidence;

    @Column(name = "evidence_ref", length = 2000)
    private String evidenceRef;

    protected Observation() {
        // JPA
    }

    public Observation(
            OperationalAsset asset,
            ObservationCategory category,
            String observationKey,
            String observationValue,
            String source,
            Instant observedAt) {
        this.asset = asset;
        this.category = category;
        this.observationKey = observationKey;
        this.observationValue = observationValue;
        this.source = source;
        this.observedAt = observedAt;
    }

    public OperationalAsset getAsset() {
        return asset;
    }

    public ObservationCategory getCategory() {
        return category;
    }

    public String getObservationKey() {
        return observationKey;
    }

    public String getObservationValue() {
        return observationValue;
    }

    public void setObservationValue(String observationValue) {
        this.observationValue = observationValue;
    }

    public String getSource() {
        return source;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getEvidenceRef() {
        return evidenceRef;
    }

    public void setEvidenceRef(String evidenceRef) {
        this.evidenceRef = evidenceRef;
    }
}
