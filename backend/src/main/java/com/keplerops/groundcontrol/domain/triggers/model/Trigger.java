package com.keplerops.groundcontrol.domain.triggers.model;

import com.keplerops.groundcontrol.domain.workflows.model.Workflow;
import com.keplerops.groundcontrol.domain.workflows.state.TriggerType;
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
import java.util.UUID;

/**
 * A trigger that starts a workflow execution.
 */
@Entity
@Table(name = "trigger")
public class Trigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;

    @Column(columnDefinition = "TEXT")
    private String config = "{}";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Trigger() {
        // JPA
    }

    public Trigger(Workflow workflow, String name, TriggerType triggerType) {
        this.workflow = workflow;
        this.name = name;
        this.triggerType = triggerType;
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

    public void recordFired() {
        this.lastFiredAt = Instant.now();
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastFiredAt() {
        return lastFiredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return name + " [" + triggerType + "]";
    }
}
