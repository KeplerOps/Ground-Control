package com.keplerops.groundcontrol.domain.executions.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.workflows.model.Workflow;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A workflow execution (run) instance.
 */
@Entity
@Table(name = "execution")
@SuppressWarnings("java:S125")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "trigger_type", length = 50)
    private String triggerType = "MANUAL";

    @Column(name = "trigger_ref")
    private String triggerRef = "";

    @Column(columnDefinition = "TEXT")
    private String inputs = "{}";

    @Column(columnDefinition = "TEXT")
    private String outputs = "{}";

    @Column(columnDefinition = "TEXT")
    private String error = "";

    private Instant startedAt;
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Execution() {
        // JPA
    }

    public Execution(Workflow workflow) {
        this.workflow = workflow;
        this.workflowVersion = workflow.getCurrentVersion();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /*@ requires newStatus != null;
    @ requires status.canTransitionTo(newStatus);
    @ ensures status == newStatus; @*/
    public void transitionStatus(ExecutionStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition execution from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current_status", status.name(), "target_status", newStatus.name()));
        }
        this.status = newStatus;
        if (newStatus == ExecutionStatus.RUNNING && startedAt == null) {
            this.startedAt = Instant.now();
        }
        if (newStatus.isTerminal()) {
            this.finishedAt = Instant.now();
            if (startedAt != null) {
                this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
            }
        }
    }

    public void markStarted() {
        transitionStatus(ExecutionStatus.QUEUED);
        transitionStatus(ExecutionStatus.RUNNING);
    }

    public void markSuccess(String outputsJson) {
        this.outputs = outputsJson;
        transitionStatus(ExecutionStatus.SUCCESS);
    }

    public void markFailed(String errorMessage) {
        this.error = errorMessage;
        transitionStatus(ExecutionStatus.FAILED);
    }

    public void cancel() {
        if (!status.isTerminal()) {
            transitionStatus(ExecutionStatus.CANCELLED);
        }
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerRef() {
        return triggerRef;
    }

    public void setTriggerRef(String triggerRef) {
        this.triggerRef = triggerRef;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public String getOutputs() {
        return outputs;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Execution[" + id + " " + status + "]";
    }
}
