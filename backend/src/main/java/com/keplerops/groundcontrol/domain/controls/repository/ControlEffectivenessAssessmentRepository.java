package com.keplerops.groundcontrol.domain.controls.repository;

import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlEffectivenessAssessmentRepository extends JpaRepository<ControlEffectivenessAssessment, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<ControlEffectivenessAssessment> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByIdAndProjectIdAndControlId(UUID id, UUID projectId, UUID controlId);

    List<ControlEffectivenessAssessment> findByProjectIdOrderByAssessedAtDesc(UUID projectId);

    List<ControlEffectivenessAssessment> findByProjectIdAndControlIdOrderByAssessedAtDesc(
            UUID projectId, UUID controlId);

    /**
     * Project-wide assessments whose {@code assessedAt <= :asOfDate}, ordered
     * by control then assessed-at desc, so callers can group by control and
     * pick the latest in one pass (GC-L007 finding #7, N+1; finding #2 as-of).
     */
    List<ControlEffectivenessAssessment> findByProjectIdAndAssessedAtLessThanEqualOrderByControlIdAscAssessedAtDesc(
            UUID projectId, LocalDate asOfDate);

    long countByProjectIdAndControlId(UUID projectId, UUID controlId);
}
