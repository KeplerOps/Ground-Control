package com.keplerops.groundcontrol.domain.assets.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.envers.Audited;

/**
 * Links an operational asset to a requirement, control, risk scenario,
 * threat-model entry, finding, evidence, audit, or external artifact.
 */
@Entity
@Audited
@Table(
        name = "asset_link",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"asset_id", "target_type", "target_identifier", "link_type"}),
            @UniqueConstraint(columnNames = {"asset_id", "target_type", "target_entity_id", "link_type"})
        })
public class AssetLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private OperationalAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private AssetLinkTargetType targetType;

    @Column(name = "target_entity_id")
    private java.util.UUID targetEntityId;

    @Column(name = "target_identifier", length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private AssetLinkType linkType;

    @Column(name = "target_url", length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", length = 255)
    private String targetTitle = "";

    protected AssetLink() {
        // JPA
    }

    public AssetLink(
            OperationalAsset asset,
            AssetLinkTargetType targetType,
            java.util.UUID targetEntityId,
            String targetIdentifier,
            AssetLinkType linkType) {
        this.asset = asset;
        this.targetType = targetType;
        this.targetEntityId = targetEntityId;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public OperationalAsset getAsset() {
        return asset;
    }

    public AssetLinkTargetType getTargetType() {
        return targetType;
    }

    public java.util.UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public AssetLinkType getLinkType() {
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
