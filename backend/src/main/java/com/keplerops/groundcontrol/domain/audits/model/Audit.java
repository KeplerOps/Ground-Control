package com.keplerops.groundcontrol.domain.audits.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * First-class audit record per GC-U001 / ADR-048.
 *
 * <p>Owns audit type, scope description, objectives, timeline phases, team
 * assignment, and the lifecycle status. Compliance frameworks, operational
 * assets, controls, risk scenarios, risk register records, evidence, and prior
 * audit findings are represented as outbound {@link AuditLink} edges, not as
 * embedded fields.
 *
 * <p>Lives in {@code domain/audits/} — distinct from {@code domain/audit/}
 * which is the Hibernate Envers infrastructure package. The REST surface uses
 * {@code /api/v1/audits/**} (plural) so it never clashes with the Envers
 * revision endpoints under {@code /api/v1/audit/**}.
 */
@Entity
@Audited
@Table(name = "audit", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class Audit extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 30)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 20)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditStatus status = AuditStatus.PLANNED;

    @Column(name = "scope_description", nullable = false, columnDefinition = "TEXT")
    private String scopeDescription;

    @Column(name = "objectives", columnDefinition = "TEXT")
    @Convert(converter = JacksonTextCollectionConverters.StringListConverter.class)
    private List<String> objectives;

    @Column(name = "phases", columnDefinition = "TEXT")
    @Convert(converter = JacksonTextCollectionConverters.AuditPhaseListConverter.class)
    private List<AuditPhase> phases;

    @Column(name = "team_members", columnDefinition = "TEXT")
    @Convert(converter = JacksonTextCollectionConverters.StringListConverter.class)
    private List<String> teamMembers;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    protected Audit() {
        // JPA
    }

    public Audit(Project project, String uid, String title, AuditType auditType, String scopeDescription) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.auditType = auditType;
        this.scopeDescription = scopeDescription;
    }

    public void transitionStatus(AuditStatus newStatus) {
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

    public AuditType getAuditType() {
        return auditType;
    }

    public void setAuditType(AuditType auditType) {
        this.auditType = auditType;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public void setScopeDescription(String scopeDescription) {
        this.scopeDescription = scopeDescription;
    }

    public List<String> getObjectives() {
        return objectives == null ? List.of() : objectives;
    }

    public void setObjectives(List<String> objectives) {
        this.objectives = objectives;
    }

    public List<AuditPhase> getPhases() {
        return phases == null ? List.of() : phases;
    }

    public void setPhases(List<AuditPhase> phases) {
        this.phases = phases;
    }

    public List<String> getTeamMembers() {
        return teamMembers == null ? List.of() : teamMembers;
    }

    public void setTeamMembers(List<String> teamMembers) {
        this.teamMembers = teamMembers;
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
