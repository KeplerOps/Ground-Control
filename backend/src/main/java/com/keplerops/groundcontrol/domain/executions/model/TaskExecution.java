package com.keplerops.groundcontrol.domain.executions.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
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
 * Execution state for an individual workflow node within a run.
 */
@Entity
@Table(name = "task_execution")
@SuppressWarnings("java:S125")
public class TaskExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private WorkflowNode node;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 30)
    private NodeType nodeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(nullable = false)
    private Integer attempt = 1;

    @Column(columnDefinition = "TEXT")
    private String inputs = "{}";

    @Column(columnDefinition = "TEXT")
    private String outputs = "{}";

    @Column(columnDefinition = "TEXT")
    private String logs = "";

    @Column(columnDefinition = "TEXT")
    private String error = "";

    private Instant startedAt;
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs = 0L;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskExecution() {
        // JPA
    }

    public TaskExecution(Execution execution, WorkflowNode node) {
        this.execution = execution;
        this.node = node;
        this.nodeName = node.getName();
        this.nodeType = node.getNodeType();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void transitionStatus(ExecutionStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition task from " + status + " to " + newStatus,
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

    public void appendLog(String log) {
        if (this.logs.isEmpty()) {
            this.logs = log;
        } else {
            this.logs = this.logs + "\n" + log;
        }
    }

    public void markSuccess(String outputsJson) {
        this.outputs = outputsJson;
        transitionStatus(ExecutionStatus.QUEUED);
        transitionStatus(ExecutionStatus.RUNNING);
        this.finishedAt = Instant.now();
        if (startedAt != null) {
            this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        this.status = ExecutionStatus.SUCCESS;
    }

    public void markRunning() {
        transitionStatus(ExecutionStatus.QUEUED);
        transitionStatus(ExecutionStatus.RUNNING);
    }

    public void markFailed(String errorMessage) {
        this.error = errorMessage;
        if (status == ExecutionStatus.PENDING) {
            transitionStatus(ExecutionStatus.QUEUED);
            transitionStatus(ExecutionStatus.RUNNING);
        } else if (status == ExecutionStatus.QUEUED) {
            transitionStatus(ExecutionStatus.RUNNING);
        }
        transitionStatus(ExecutionStatus.FAILED);
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Execution getExecution() {
        return execution;
    }

    public WorkflowNode getNode() {
        return node;
    }

    public String getNodeName() {
        return nodeName;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
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

    public String getLogs() {
        return logs;
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
        return nodeName + " [" + status + "]";
    }
}
