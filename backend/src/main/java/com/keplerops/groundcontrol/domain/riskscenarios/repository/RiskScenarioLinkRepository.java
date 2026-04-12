package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskScenarioLinkRepository extends JpaRepository<RiskScenarioLink, UUID> {

    List<RiskScenarioLink> findByRiskScenarioId(UUID riskScenarioId);

    @Query("SELECT l FROM RiskScenarioLink l JOIN FETCH l.riskScenario WHERE l.riskScenario.project.id = :projectId")
    List<RiskScenarioLink> findByProjectId(@Param("projectId") UUID projectId);

    List<RiskScenarioLink> findByRiskScenarioIdAndTargetType(
            UUID riskScenarioId, RiskScenarioLinkTargetType targetType);

    Optional<RiskScenarioLink> findByIdAndRiskScenarioProjectId(UUID id, UUID projectId);

    boolean existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            String targetIdentifier,
            RiskScenarioLinkType linkType);

    boolean existsByRiskScenarioIdAndTargetTypeAndTargetEntityIdAndLinkType(
            UUID riskScenarioId,
            RiskScenarioLinkTargetType targetType,
            UUID targetEntityId,
            RiskScenarioLinkType linkType);

    @Query("SELECT l FROM RiskScenarioLink l JOIN FETCH l.riskScenario"
            + " WHERE l.targetType = :targetType AND l.targetEntityId = :targetEntityId"
            + " AND l.riskScenario.project.id = :projectId")
    List<RiskScenarioLink> findByTargetTypeAndTargetEntityIdAndProjectId(
            @Param("targetType") RiskScenarioLinkTargetType targetType,
            @Param("targetEntityId") UUID targetEntityId,
            @Param("projectId") UUID projectId);
}
