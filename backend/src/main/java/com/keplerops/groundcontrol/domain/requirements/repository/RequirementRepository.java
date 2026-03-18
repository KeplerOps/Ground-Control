package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequirementRepository extends JpaRepository<Requirement, UUID>, JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByUid(String uid);

    boolean existsByUid(String uid);

    Optional<Requirement> findByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    List<Requirement> findByProjectId(UUID projectId);

    List<Requirement> findByProjectIdAndArchivedAtIsNull(UUID projectId);
}
