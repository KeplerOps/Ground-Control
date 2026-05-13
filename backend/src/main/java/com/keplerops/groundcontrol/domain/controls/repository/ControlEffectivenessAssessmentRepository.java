package com.keplerops.groundcontrol.domain.controls.repository;

import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlEffectivenessAssessmentRepository extends JpaRepository<ControlEffectivenessAssessment, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<ControlEffectivenessAssessment> findByIdAndProjectId(UUID id, UUID projectId);

    List<ControlEffectivenessAssessment> findByProjectIdOrderByAssessedAtDesc(UUID projectId);

    List<ControlEffectivenessAssessment> findByProjectIdAndControlIdOrderByAssessedAtDesc(
            UUID projectId, UUID controlId);

    long countByProjectIdAndControlId(UUID projectId, UUID controlId);
}
