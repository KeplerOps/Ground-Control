package com.keplerops.groundcontrol.domain.controls.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
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

@Entity
@Audited
@Table(
        name = "control_link",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"control_id", "target_type", "target_identifier", "link_type"}),
            @UniqueConstraint(columnNames = {"control_id", "target_type", "target_entity_id", "link_type"})
        })
public class ControlLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ControlLinkTargetType targetType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(name = "target_identifier", length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private ControlLinkType linkType;

    @Column(name = "target_url", length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", length = 255)
    private String targetTitle = "";

    protected ControlLink() {
        // JPA
    }

    public ControlLink(
            Control control,
            ControlLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            ControlLinkType linkType) {
        this.control = control;
        this.targetType = targetType;
        this.targetEntityId = targetEntityId;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public Control getControl() {
        return control;
    }

    public ControlLinkTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public ControlLinkType getLinkType() {
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
