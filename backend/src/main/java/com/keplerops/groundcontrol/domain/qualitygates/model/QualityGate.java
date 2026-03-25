package com.keplerops.groundcontrol.domain.qualitygates.model;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
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
import java.util.UUID;

@Entity
@Table(name = "quality_gate")
public class QualityGate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 30)
    private MetricType metricType;

    @Column(name = "metric_param", length = 30)
    private String metricParam;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_status", length = 20)
    private Status scopeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private ComparisonOperator operator;

    @Column(nullable = false)
    private double threshold;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected QualityGate() {
        // JPA
    }

    public QualityGate(
            Project project,
            String name,
            String description,
            MetricType metricType,
            String metricParam,
            Status scopeStatus,
            ComparisonOperator operator,
            double threshold) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.metricType = metricType;
        this.metricParam = metricParam;
        this.scopeStatus = scopeStatus;
        this.operator = operator;
        this.threshold = threshold;
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

    public UUID getId() {
        return id;
    }

    public Project getProject() {
        return project;
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

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public String getMetricParam() {
        return metricParam;
    }

    public void setMetricParam(String metricParam) {
        this.metricParam = metricParam;
    }

    public Status getScopeStatus() {
        return scopeStatus;
    }

    public void setScopeStatus(Status scopeStatus) {
        this.scopeStatus = scopeStatus;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
