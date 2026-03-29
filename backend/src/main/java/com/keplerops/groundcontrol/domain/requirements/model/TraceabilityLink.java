package com.keplerops.groundcontrol.domain.requirements.model;

import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.envers.Audited;

/**
 * Links a requirement to an external artifact for traceability.
 */
@Entity
@Audited
@Table(
        name = "traceability_link",
        uniqueConstraints =
                @UniqueConstraint(
                        columnNames = {"requirement_id", "artifact_type", "artifact_identifier", "link_type"}))
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class TraceabilityLink {

    /*@ public invariant requirement != null;
    @ public invariant artifactType != null;
    @ public invariant artifactIdentifier != null && !artifactIdentifier.isEmpty();
    @ public invariant linkType != null;
    @ public invariant syncStatus != null;
    @ public invariant artifactUrl != null;
    @ public invariant artifactTitle != null; @*/

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 30)
    private ArtifactType artifactType;

    @Column(name = "artifact_identifier", nullable = false, length = 500)
    private String artifactIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private LinkType linkType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 10)
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column(name = "artifact_url", length = 2000)
    private String artifactUrl = "";

    @Column(name = "artifact_title", length = 255)
    private String artifactTitle = "";

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TraceabilityLink() {
        // JPA
    }

    /*@ requires requirement != null;
    @ requires artifactType != null;
    @ requires artifactIdentifier != null && !artifactIdentifier.isEmpty();
    @ requires linkType != null;
    @ ensures this.requirement == requirement;
    @ ensures this.artifactType == artifactType;
    @ ensures this.artifactIdentifier.equals(artifactIdentifier);
    @ ensures this.linkType == linkType;
    @ ensures this.syncStatus == SyncStatus.SYNCED; @*/
    public TraceabilityLink(
            Requirement requirement, ArtifactType artifactType, String artifactIdentifier, LinkType linkType) {
        this.requirement = requirement;
        this.artifactType = artifactType;
        this.artifactIdentifier = artifactIdentifier;
        this.linkType = linkType;
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

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public String getArtifactIdentifier() {
        return artifactIdentifier;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    /*@ requires syncStatus != null;
    @ ensures this.syncStatus == syncStatus; @*/
    public void setSyncStatus(/*@ non_null @*/ SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getArtifactUrl() {
        return artifactUrl;
    }

    /*@ requires artifactUrl != null;
    @ ensures this.artifactUrl == artifactUrl; @*/
    public void setArtifactUrl(/*@ non_null @*/ String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public String getArtifactTitle() {
        return artifactTitle;
    }

    /*@ requires artifactTitle != null;
    @ ensures this.artifactTitle == artifactTitle; @*/
    public void setArtifactTitle(/*@ non_null @*/ String artifactTitle) {
        this.artifactTitle = artifactTitle;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TraceabilityLink other)) return false;
        return artifactType != null
                && artifactIdentifier != null
                && linkType != null
                && Objects.equals(requirement != null ? requirement.getId() : null, other.requirement != null ? other.requirement.getId() : null)
                && artifactType.equals(other.artifactType)
                && artifactIdentifier.equals(other.artifactIdentifier)
                && linkType.equals(other.linkType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                requirement != null ? requirement.getId() : null,
                artifactType,
                artifactIdentifier,
                linkType);
    }
}
