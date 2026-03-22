package com.keplerops.groundcontrol.domain.baselines.model;

import com.keplerops.groundcontrol.domain.projects.model.Project;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "baseline")
public class Baseline {

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

    @Column(nullable = false)
    private int revisionNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 100)
    private String createdBy;

    protected Baseline() {
        // JPA
    }

    public Baseline(Project project, String name, String description, int revisionNumber, String createdBy) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.revisionNumber = revisionNumber;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
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

    public String getDescription() {
        return description;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return name;
    }
}
