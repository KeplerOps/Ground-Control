package com.keplerops.groundcontrol.domain.threatmodels.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import jakarta.persistence.Column;
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
 * First-class threat modeling entry per GC-H001 and ADR-024.
 *
 * <p>Distinguishes threat source, threat event, and effect, with optional STRIDE
 * taxonomy. Separate aggregate from risk scenarios / risk register / risk
 * assessments / treatment plans — no likelihood, impact, residual risk,
 * approval, or treatment state lives here.
 *
 * <p>Affected operational assets or system boundaries are represented by
 * {@link ThreatModelLink} edges with
 * {@link com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType#ASSET}
 * targets. The narrative field is for analyst context only and is not
 * authoritative scope.
 */
@Entity
@Audited
@Table(name = "threat_model", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class ThreatModel extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 30)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThreatModelStatus status = ThreatModelStatus.DRAFT;

    @Column(name = "threat_source", nullable = false, columnDefinition = "TEXT")
    private String threatSource;

    @Column(name = "threat_event", nullable = false, columnDefinition = "TEXT")
    private String threatEvent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String effect;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StrideCategory stride;

    @Column(columnDefinition = "TEXT")
    private String narrative;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    protected ThreatModel() {
        // JPA
    }

    public ThreatModel(
            Project project, String uid, String title, String threatSource, String threatEvent, String effect) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.threatSource = threatSource;
        this.threatEvent = threatEvent;
        this.effect = effect;
    }

    public void transitionStatus(ThreatModelStatus newStatus) {
        if (newStatus == null) {
            throw new DomainValidationException("Target status must not be null");
        }
        if (!this.status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition from " + this.status + " to " + newStatus,
                    "validation_error",
                    Map.of(
                            "current_status",
                            this.status.name(),
                            "target_status",
                            newStatus.name(),
                            "valid_targets",
                            this.status.validTargets().toString()));
        }
        this.status = newStatus;
    }

    public Project getProject() {
        return project;
    }

    public String getUid() {
        return uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ThreatModelStatus getStatus() {
        return status;
    }

    public String getThreatSource() {
        return threatSource;
    }

    public void setThreatSource(String threatSource) {
        this.threatSource = threatSource;
    }

    public String getThreatEvent() {
        return threatEvent;
    }

    public void setThreatEvent(String threatEvent) {
        this.threatEvent = threatEvent;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public StrideCategory getStride() {
        return stride;
    }

    public void setStride(StrideCategory stride) {
        this.stride = stride;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return uid + ": " + title;
    }
}
