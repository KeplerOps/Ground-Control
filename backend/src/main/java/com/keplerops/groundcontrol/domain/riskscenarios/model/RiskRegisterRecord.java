package com.keplerops.groundcontrol.domain.riskscenarios.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "risk_register_record", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class RiskRegisterRecord extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskRegisterStatus status = RiskRegisterStatus.IDENTIFIED;

    @Column(name = "review_cadence", length = 100)
    private String reviewCadence;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Convert(converter = JacksonTextCollectionConverters.StringListConverter.class)
    @Column(name = "category_tags", columnDefinition = "TEXT")
    private java.util.List<String> categoryTags;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(name = "decision_metadata", columnDefinition = "TEXT")
    private Map<String, Object> decisionMetadata;

    @Column(name = "asset_scope_summary", columnDefinition = "TEXT")
    private String assetScopeSummary;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "risk_register_record_scenario",
            joinColumns = @JoinColumn(name = "risk_register_record_id"),
            inverseJoinColumns = @JoinColumn(name = "risk_scenario_id"))
    private Set<RiskScenario> riskScenarios = new LinkedHashSet<>();

    protected RiskRegisterRecord() {
        // JPA
    }

    public RiskRegisterRecord(Project project, String uid, String title) {
        this.project = project;
        this.uid = uid;
        this.title = title;
    }

    public void transitionStatus(RiskRegisterStatus newStatus) {
        if (newStatus == null || !status.canTransitionTo(newStatus)) {
            throw new DomainValidationException("Cannot transition from " + status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public void replaceRiskScenarios(java.util.Collection<RiskScenario> scenarios) {
        this.riskScenarios.clear();
        this.riskScenarios.addAll(scenarios);
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public RiskRegisterStatus getStatus() {
        return status;
    }

    public String getReviewCadence() {
        return reviewCadence;
    }

    public void setReviewCadence(String reviewCadence) {
        this.reviewCadence = reviewCadence;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(Instant nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public java.util.List<String> getCategoryTags() {
        return categoryTags;
    }

    public void setCategoryTags(java.util.List<String> categoryTags) {
        this.categoryTags = categoryTags;
    }

    public Map<String, Object> getDecisionMetadata() {
        return decisionMetadata;
    }

    public void setDecisionMetadata(Map<String, Object> decisionMetadata) {
        this.decisionMetadata = decisionMetadata;
    }

    public String getAssetScopeSummary() {
        return assetScopeSummary;
    }

    public void setAssetScopeSummary(String assetScopeSummary) {
        this.assetScopeSummary = assetScopeSummary;
    }

    public Set<RiskScenario> getRiskScenarios() {
        return riskScenarios;
    }
}
