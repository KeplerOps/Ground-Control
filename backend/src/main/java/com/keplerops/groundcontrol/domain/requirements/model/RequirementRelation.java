package com.keplerops.groundcontrol.domain.requirements.model;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.envers.Audited;

/**
 * A directed relationship between two requirements.
 */
@Entity
@Audited
@Table(
        name = "requirement_relation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_id", "target_id", "relation_type"}))
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class RequirementRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Requirement source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private Requirement target;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RequirementRelation() {
        // JPA
    }

    /*@ requires source != null && target != null && relationType != null;
    @ requires !source.getId().equals(target.getId());
    @ ensures this.source == source;
    @ ensures this.target == target;
    @ ensures this.relationType == relationType; @*/
    public RequirementRelation(Requirement source, Requirement target, RelationType relationType) {
        if (source.getId() != null && source.getId().equals(target.getId())) {
            throw new DomainValidationException("A requirement cannot relate to itself");
        }
        this.source = source;
        this.target = target;
        this.relationType = relationType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Requirement getSource() {
        return source;
    }

    public Requirement getTarget() {
        return target;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequirementRelation other)) return false;
        return relationType != null
                && Objects.equals(source, other.source)
                && Objects.equals(target, other.target)
                && relationType.equals(other.relationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, relationType);
    }

    @Override
    public String toString() {
        return source.getUid() + " --[" + relationType + "]--> " + target.getUid();
    }
}
