package com.keplerops.groundcontrol.domain.threatmodels.repository;

import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThreatModelLinkRepository extends JpaRepository<ThreatModelLink, UUID> {

    List<ThreatModelLink> findByThreatModelId(UUID threatModelId);

    @Query("SELECT l FROM ThreatModelLink l JOIN FETCH l.threatModel WHERE l.threatModel.project.id = :projectId")
    List<ThreatModelLink> findByProjectId(@Param("projectId") UUID projectId);

    Optional<ThreatModelLink> findByIdAndThreatModelProjectId(UUID id, UUID projectId);

    boolean existsByThreatModelIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID threatModelId,
            ThreatModelLinkTargetType targetType,
            String targetIdentifier,
            ThreatModelLinkType linkType);

    boolean existsByThreatModelIdAndTargetTypeAndTargetEntityIdAndLinkType(
            UUID threatModelId,
            ThreatModelLinkTargetType targetType,
            UUID targetEntityId,
            ThreatModelLinkType linkType);
}
