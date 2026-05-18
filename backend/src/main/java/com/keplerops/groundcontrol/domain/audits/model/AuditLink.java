package com.keplerops.groundcontrol.domain.audits.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
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
 * Dual-mode link from an audit to an internal first-class entity (via
 * {@code targetEntityId}) or an external/not-yet-modeled artifact (via
 * {@code targetIdentifier}) per ADR-048.
 */
@Entity
@Audited
@Table(
        name = "audit_link",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"audit_id", "target_type", "target_identifier", "link_type"}),
            @UniqueConstraint(columnNames = {"audit_id", "target_type", "target_entity_id", "link_type"})
        })
public class AuditLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AuditLinkTargetType targetType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_identifier", length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private AuditLinkType linkType;

    @Column(name = "target_url", nullable = false, length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", nullable = false, length = 255)
    private String targetTitle = "";

    protected AuditLink() {
        // JPA
    }

    public AuditLink(
            Audit audit,
            AuditLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            AuditLinkType linkType) {
        this.audit = audit;
        this.targetType = targetType;
        this.targetEntityId = targetEntityId;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public Audit getAudit() {
        return audit;
    }

    public AuditLinkTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public AuditLinkType getLinkType() {
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
