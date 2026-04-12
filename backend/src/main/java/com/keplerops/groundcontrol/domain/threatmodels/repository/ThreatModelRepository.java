package com.keplerops.groundcontrol.domain.threatmodels.repository;

import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreatModelRepository extends JpaRepository<ThreatModel, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<ThreatModel> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<ThreatModel> findByProjectIdAndUid(UUID projectId, String uid);

    List<ThreatModel> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
