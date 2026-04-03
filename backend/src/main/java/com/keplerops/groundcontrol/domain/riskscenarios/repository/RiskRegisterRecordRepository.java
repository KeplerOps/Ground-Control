package com.keplerops.groundcontrol.domain.riskscenarios.repository;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskRegisterRecordRepository extends JpaRepository<RiskRegisterRecord, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    @Query("SELECT DISTINCT r FROM RiskRegisterRecord r LEFT JOIN FETCH r.riskScenarios"
            + " WHERE r.id = :id AND r.project.id = :projectId")
    Optional<RiskRegisterRecord> findByIdAndProjectIdWithScenarios(
            @Param("id") UUID id, @Param("projectId") UUID projectId);

    @Query("SELECT DISTINCT r FROM RiskRegisterRecord r LEFT JOIN FETCH r.riskScenarios"
            + " WHERE r.project.id = :projectId ORDER BY r.createdAt DESC")
    List<RiskRegisterRecord> findByProjectIdWithScenariosOrderByCreatedAtDesc(@Param("projectId") UUID projectId);
}
