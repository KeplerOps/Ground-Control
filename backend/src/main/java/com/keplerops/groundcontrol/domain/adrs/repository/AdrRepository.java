package com.keplerops.groundcontrol.domain.adrs.repository;

import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdrRepository extends JpaRepository<ArchitectureDecisionRecord, UUID> {

    List<ArchitectureDecisionRecord> findByProjectIdOrderByDecisionDateDesc(UUID projectId);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    Optional<ArchitectureDecisionRecord> findByProjectIdAndUid(UUID projectId, String uid);
}
