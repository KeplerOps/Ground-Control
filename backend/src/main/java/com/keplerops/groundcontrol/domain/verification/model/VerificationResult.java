package com.keplerops.groundcontrol.domain.verification.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
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
import java.time.Instant;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "verification_result")
public class VerificationResult extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_id")
    private TraceabilityLink target;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @Column(nullable = false, length = 100)
    private String prover;

    @Column(columnDefinition = "TEXT")
    private String property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus result;

    @Enumerated(EnumType.STRING)
    @Column(name = "assurance_level", nullable = false, length = 5)
    private AssuranceLevel assuranceLevel;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> evidence;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected VerificationResult() {
        // JPA
    }

    public VerificationResult(
            Project project,
            String prover,
            VerificationStatus result,
            AssuranceLevel assuranceLevel,
            Instant verifiedAt) {
        this.project = project;
        this.prover = prover;
        this.result = result;
        this.assuranceLevel = assuranceLevel;
        this.verifiedAt = verifiedAt;
    }

    public Project getProject() {
        return project;
    }

    public TraceabilityLink getTarget() {
        return target;
    }

    public void setTarget(TraceabilityLink target) {
        this.target = target;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    public String getProver() {
        return prover;
    }

    public void setProver(String prover) {
        this.prover = prover;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public VerificationStatus getResult() {
        return result;
    }

    public void setResult(VerificationStatus result) {
        this.result = result;
    }

    public AssuranceLevel getAssuranceLevel() {
        return assuranceLevel;
    }

    public void setAssuranceLevel(AssuranceLevel assuranceLevel) {
        this.assuranceLevel = assuranceLevel;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
