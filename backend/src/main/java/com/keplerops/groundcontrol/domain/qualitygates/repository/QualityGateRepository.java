package com.keplerops.groundcontrol.domain.qualitygates.repository;

import com.keplerops.groundcontrol.domain.qualitygates.model.QualityGate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityGateRepository extends JpaRepository<QualityGate, UUID> {

    List<QualityGate> findByProjectIdAndEnabledTrueOrderByNameAsc(UUID projectId);

    List<QualityGate> findByProjectIdOrderByNameAsc(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);
}
