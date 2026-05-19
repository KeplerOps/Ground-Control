package com.keplerops.groundcontrol.domain.audits.repository;

import com.keplerops.groundcontrol.domain.audits.model.Audit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<Audit> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<Audit> findByProjectIdAndUid(UUID projectId, String uid);

    List<Audit> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
