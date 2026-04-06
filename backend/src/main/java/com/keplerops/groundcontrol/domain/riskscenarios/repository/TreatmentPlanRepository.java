package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.TreatmentPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<TreatmentPlan> findByIdAndProjectId(UUID id, UUID projectId);

    List<TreatmentPlan> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<TreatmentPlan> findByProjectIdAndRiskRegisterRecordIdOrderByCreatedAtDesc(
            UUID projectId, UUID riskRegisterRecordId);
}
