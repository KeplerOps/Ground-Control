package com.keplerops.groundcontrol.domain.riskscenarios.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
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
 * A scoped statement of potential future loss tied to one or more affected
 * operational assets, boundaries, processes, systems, objectives, or third
 * parties within a defined time horizon. Anchors risk to scenario rather
 * than a vague label, supporting FAIR, NIST SP 800-30, and ISO-style
 * risk methods.
 */
@Entity
@Audited
@Table(name = "risk_scenario", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class RiskScenario extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 20)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskScenarioStatus status = RiskScenarioStatus.DRAFT;

    @Column(name = "threat_source", nullable = false, columnDefinition = "TEXT")
    private String threatSource;

    @Column(name = "threat_event", nullable = false, columnDefinition = "TEXT")
    private String threatEvent;

    @Column(name = "affected_object", nullable = false, columnDefinition = "TEXT")
    private String affectedObject;

    @Column(columnDefinition = "TEXT")
    private String vulnerability;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String consequence;

    @Column(name = "time_horizon", nullable = false, length = 100)
    private String timeHorizon;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    protected RiskScenario() {
        // JPA
    }

    public RiskScenario(
            Project project,
            String uid,
            String title,
            String threatSource,
            String threatEvent,
            String affectedObject,
            String consequence) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.threatSource = threatSource;
        this.threatEvent = threatEvent;
        this.affectedObject = affectedObject;
        this.consequence = consequence;
    }

    public void transitionStatus(RiskScenarioStatus newStatus) {
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

    public RiskScenarioStatus getStatus() {
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

    public String getAffectedObject() {
        return affectedObject;
    }

    public void setAffectedObject(String affectedObject) {
        this.affectedObject = affectedObject;
    }

    public String getVulnerability() {
        return vulnerability;
    }

    public void setVulnerability(String vulnerability) {
        this.vulnerability = vulnerability;
    }

    public String getConsequence() {
        return consequence;
    }

    public void setConsequence(String consequence) {
        this.consequence = consequence;
    }

    public String getTimeHorizon() {
        return timeHorizon;
    }

    public void setTimeHorizon(String timeHorizon) {
        this.timeHorizon = timeHorizon;
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
