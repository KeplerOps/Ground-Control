package com.keplerops.groundcontrol.domain.requirements.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * A traceable requirement with lifecycle management.
 *
 * <p>Uses soft-delete via {@link #archive()} -- the default query should
 * filter on {@code archivedAt IS NULL}.
 */
@Entity
@Audited
@Table(name = "requirement")
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class Requirement {

    /*@ public invariant archivedAt == null || status == Status.ARCHIVED; @*/

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(columnDefinition = "TEXT")
    private String rationale = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementType requirementType = RequirementType.FUNCTIONAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MUST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    private Integer wave;

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant archivedAt;

    protected Requirement() {
        // JPA
    }

    public Requirement(Project project, String uid, String title, String statement) {
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.statement = statement;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Transition the requirement to a new lifecycle status.
     */
    /*@ requires newStatus != null;
    @ requires status.canTransitionTo(newStatus);
    @ ensures status == newStatus; @*/
    public void transitionStatus(/*@ non_null @*/ Status newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current_status", status.name(), "target_status", newStatus.name()));
        }
        this.status = newStatus;
        if (newStatus == Status.ARCHIVED) {
            this.archivedAt = Instant.now();
        }
    }

    /**
     * Soft-delete this requirement by transitioning to ARCHIVED.
     * Idempotent if already archived.
     */
    /*@ requires status == Status.ARCHIVED || status.canTransitionTo(Status.ARCHIVED);
    @ ensures status == Status.ARCHIVED;
    @ ensures archivedAt != null || \old(status) == Status.ARCHIVED; @*/
    public void archive() {
        if (this.status == Status.ARCHIVED) {
            return;
        }
        transitionStatus(Status.ARCHIVED);
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
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

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public RequirementType getRequirementType() {
        return requirementType;
    }

    public void setRequirementType(RequirementType requirementType) {
        this.requirementType = requirementType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getWave() {
        return wave;
    }

    public void setWave(Integer wave) {
        this.wave = wave;
    }

    public String getCustomFields() {
        return customFields;
    }

    public void setCustomFields(String customFields) {
        this.customFields = customFields;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    @Override
    public String toString() {
        return uid;
    }
}
