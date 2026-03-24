package com.keplerops.groundcontrol.domain.workflows.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import com.keplerops.groundcontrol.domain.workspaces.model.Workspace;
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

/**
 * A workflow definition — a DAG of nodes with lifecycle management.
 */
@Entity
@Table(name = "workflow")
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(nullable = false)
    private Integer currentVersion = 0;

    @Column(columnDefinition = "TEXT")
    private String tags = "";

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 3600;

    @Column(name = "max_retries")
    private Integer maxRetries = 0;

    @Column(name = "retry_backoff_ms")
    private Integer retryBackoffMs = 1000;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Workflow() {
        // JPA
    }

    public Workflow(Workspace workspace, String name) {
        this.workspace = workspace;
        this.name = name;
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

    /*@ requires newStatus != null;
    @ requires status.canTransitionTo(newStatus);
    @ ensures status == newStatus; @*/
    public void transitionStatus(/*@ non_null @*/ WorkflowStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition workflow from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current_status", status.name(), "target_status", newStatus.name()));
        }
        this.status = newStatus;
    }

    public void publish() {
        if (status == WorkflowStatus.DRAFT) {
            transitionStatus(WorkflowStatus.ACTIVE);
        }
        this.currentVersion++;
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(Integer retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return name + " (v" + currentVersion + ")";
    }
}
