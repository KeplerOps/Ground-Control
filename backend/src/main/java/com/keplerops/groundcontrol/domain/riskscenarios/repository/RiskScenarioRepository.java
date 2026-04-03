package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskScenarioRepository extends JpaRepository<RiskScenario, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<RiskScenario> findByProjectIdAndUid(UUID projectId, String uid);

    List<RiskScenario> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
