package com.keplerops.groundcontrol.domain.workflows.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * A directed edge between two workflow nodes.
 */
@Entity
@Table(
        name = "workflow_edge",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"workflow_id", "source_node_id", "target_node_id"}))
public class WorkflowEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_node_id", nullable = false)
    private WorkflowNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_node_id", nullable = false)
    private WorkflowNode targetNode;

    @Column(name = "condition_expr", columnDefinition = "TEXT")
    private String conditionExpr = "";

    @Column
    private String label = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowEdge() {
        // JPA
    }

    public WorkflowEdge(Workflow workflow, WorkflowNode sourceNode, WorkflowNode targetNode) {
        if (sourceNode.getId() != null && sourceNode.getId().equals(targetNode.getId())) {
            throw new DomainValidationException("A node cannot connect to itself");
        }
        this.workflow = workflow;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public WorkflowNode getSourceNode() {
        return sourceNode;
    }

    public WorkflowNode getTargetNode() {
        return targetNode;
    }

    public String getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return sourceNode.getName() + " --> " + targetNode.getName();
    }
}
