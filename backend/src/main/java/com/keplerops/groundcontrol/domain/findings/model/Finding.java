package com.keplerops.groundcontrol.domain.findings.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
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
import org.hibernate.envers.NotAudited;

/**
 * First-class GRC issue record per GC-V001 and ADR-038.
 *
 * <p>Owns finding type, severity, description, root-cause analysis, owner, due
 * date, and the lifecycle status. Affected controls, risks, assets,
 * observations, evidence, audits, and remediation plans are represented as
 * outbound {@link FindingLink} edges, not as embedded fields.
 *
 * <p>Distinct aggregate from {@code Observation} (raw evidence about an asset),
 * {@code Control} (the expected safeguard), and the risk-management aggregates
 * — those are link targets, not finding fields.
 */
@Entity
@Audited
@Table(name = "finding", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class Finding extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 30)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false, length = 30)
    private FindingType findingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FindingStatus status = FindingStatus.OPEN;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "root_cause_analysis", columnDefinition = "TEXT")
    private String rootCauseAnalysis;

    @Column(length = 100)
    private String owner;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    protected Finding() {
        // JPA
    }

    public Finding(
            Project project,
            String uid,
            String title,
            FindingType findingType,
            FindingSeverity severity,
            String description) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.findingType = findingType;
        this.severity = severity;
        this.description = description;
    }

    public void transitionStatus(FindingStatus newStatus) {
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

    public FindingType getFindingType() {
        return findingType;
    }

    public void setFindingType(FindingType findingType) {
        this.findingType = findingType;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(FindingSeverity severity) {
        this.severity = severity;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRootCauseAnalysis() {
        return rootCauseAnalysis;
    }

    public void setRootCauseAnalysis(String rootCauseAnalysis) {
        this.rootCauseAnalysis = rootCauseAnalysis;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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
