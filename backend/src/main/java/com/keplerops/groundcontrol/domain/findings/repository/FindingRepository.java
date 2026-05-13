package com.keplerops.groundcontrol.domain.findings.repository;

import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<Finding> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<Finding> findByProjectIdAndUid(UUID projectId, String uid);

    List<Finding> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    @Query("SELECT f.id FROM Finding f WHERE f.project.id = :projectId AND f.status <> :excludedStatus")
    List<UUID> findIdsByProjectIdAndStatusNot(
            @Param("projectId") UUID projectId, @Param("excludedStatus") FindingStatus excludedStatus);
}
