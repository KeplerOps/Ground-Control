package com.keplerops.groundcontrol.domain.findings.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
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
 * Dual-mode link from a finding to an internal first-class entity (via
 * {@code targetEntityId}) or an external/not-yet-modeled artifact (via
 * {@code targetIdentifier}) per ADR-038.
 */
@Entity
@Audited
@Table(
        name = "finding_link",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"finding_id", "target_type", "target_identifier", "link_type"}),
            @UniqueConstraint(columnNames = {"finding_id", "target_type", "target_entity_id", "link_type"})
        })
public class FindingLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private FindingLinkTargetType targetType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_identifier", length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private FindingLinkType linkType;

    @Column(name = "target_url", nullable = false, length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", nullable = false, length = 255)
    private String targetTitle = "";

    protected FindingLink() {
        // JPA
    }

    public FindingLink(
            Finding finding,
            FindingLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            FindingLinkType linkType) {
        this.finding = finding;
        this.targetType = targetType;
        this.targetEntityId = targetEntityId;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public Finding getFinding() {
        return finding;
    }

    public FindingLinkTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public FindingLinkType getLinkType() {
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
