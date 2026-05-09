package com.keplerops.groundcontrol.domain.threatmodels.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.envers.Audited;

/**
 * Dual-mode link from a threat model entry to an internal first-class entity
 * (via {@code targetEntityId}) or an external/not-yet-modeled artifact
 * (via {@code targetIdentifier}) per ADR-024 §4.
 */
@Entity
@Audited
@Table(
        name = "threat_model_link",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"threat_model_id", "target_type", "target_identifier", "link_type"}),
            @UniqueConstraint(columnNames = {"threat_model_id", "target_type", "target_entity_id", "link_type"})
        })
public class ThreatModelLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "threat_model_id", nullable = false)
    private ThreatModel threatModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private ThreatModelLinkTargetType targetType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_identifier", length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private ThreatModelLinkType linkType;

    @Column(name = "target_url", length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", length = 255)
    private String targetTitle = "";

    protected ThreatModelLink() {
        // JPA
    }

    public ThreatModelLink(
            ThreatModel threatModel,
            ThreatModelLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            ThreatModelLinkType linkType) {
        this.threatModel = threatModel;
        this.targetType = targetType;
        this.targetEntityId = targetEntityId;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public ThreatModel getThreatModel() {
        return threatModel;
    }

    public ThreatModelLinkTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public ThreatModelLinkType getLinkType() {
        return linkType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }
}
