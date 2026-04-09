package com.keplerops.groundcontrol.domain.packregistry.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.projects.model.Project;
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
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(
        name = "trust_policy",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_trust_policy_project_name",
                        columnNames = {"project_id", "name"}))
public class TrustPolicy extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_outcome", nullable = false, length = 20)
    private TrustOutcome defaultOutcome;

    @Convert(converter = JacksonTextCollectionConverters.TrustPolicyRuleListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<TrustPolicyRule> rules;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled;

    protected TrustPolicy() {}

    public TrustPolicy(Project project, String name, TrustOutcome defaultOutcome) {
        this.project = project;
        this.name = name;
        this.defaultOutcome = defaultOutcome;
        this.enabled = true;
        this.priority = 0;
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

    public TrustOutcome getDefaultOutcome() {
        return defaultOutcome;
    }

    public void setDefaultOutcome(TrustOutcome defaultOutcome) {
        this.defaultOutcome = defaultOutcome;
    }

    public List<TrustPolicyRule> getRules() {
        return rules;
    }

    public void setRules(List<TrustPolicyRule> rules) {
        this.rules = rules;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
