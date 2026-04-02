package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskScenarioLinkRepository extends JpaRepository<RiskScenarioLink, UUID> {

    List<RiskScenarioLink> findByRiskScenarioId(UUID riskScenarioId);

    List<RiskScenarioLink> findByRiskScenarioIdAndTargetType(
            UUID riskScenarioId, RiskScenarioLinkTargetType targetType);

    boolean existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            String targetIdentifier,
            RiskScenarioLinkType linkType);
}
