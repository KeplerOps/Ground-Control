package com.keplerops.groundcontrol.domain.riskscenarios.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.envers.Audited;

/**
 * Links a risk scenario to a threat model, vulnerability, control, finding,
 * evidence, audit record, risk register entry, observation, asset,
 * requirement, or external artifact.
 */
@Entity
@Audited
@Table(
        name = "risk_scenario_link",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"risk_scenario_id", "target_type", "target_identifier", "link_type"}))
public class RiskScenarioLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_scenario_id", nullable = false)
    private RiskScenario riskScenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private RiskScenarioLinkTargetType targetType;

    @Column(name = "target_identifier", nullable = false, length = 500)
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private RiskScenarioLinkType linkType;

    @Column(name = "target_url", length = 2000)
    private String targetUrl = "";

    @Column(name = "target_title", length = 255)
    private String targetTitle = "";

    protected RiskScenarioLink() {
        // JPA
    }

    public RiskScenarioLink(
            RiskScenario riskScenario,
            RiskScenarioLinkTargetType targetType,
            String targetIdentifier,
            RiskScenarioLinkType linkType) {
        this.riskScenario = riskScenario;
        this.targetType = targetType;
        this.targetIdentifier = targetIdentifier;
        this.linkType = linkType;
    }

    public RiskScenario getRiskScenario() {
        return riskScenario;
    }

    public RiskScenarioLinkTargetType getTargetType() {
        return targetType;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public RiskScenarioLinkType getLinkType() {
        return linkType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }
}
