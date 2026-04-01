package com.keplerops.groundcontrol.domain.adrs.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(
        name = "architecture_decision_record",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class ArchitectureDecisionRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 20)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdrStatus status = AdrStatus.PROPOSED;

    @Column(name = "decision_date", nullable = false)
    private LocalDate decisionDate;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(columnDefinition = "TEXT")
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String consequences;

    @Column(name = "superseded_by", length = 20)
    private String supersededBy;

    @Column(length = 100)
    private String createdBy;

    protected ArchitectureDecisionRecord() {
        // JPA
    }

    public ArchitectureDecisionRecord(
            Project project,
            String uid,
            String title,
            LocalDate decisionDate,
            String context,
            String decision,
            String consequences,
            String createdBy) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.decisionDate = decisionDate;
        this.context = context;
        this.decision = decision;
        this.consequences = consequences;
        this.createdBy = createdBy;
    }

    public void transitionStatus(AdrStatus newStatus) {
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

    public AdrStatus getStatus() {
        return status;
    }

    public LocalDate getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDate decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getConsequences() {
        return consequences;
    }

    public void setConsequences(String consequences) {
        this.consequences = consequences;
    }

    public String getSupersededBy() {
        return supersededBy;
    }

    public void setSupersededBy(String supersededBy) {
        this.supersededBy = supersededBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return uid + ": " + title;
    }
}
