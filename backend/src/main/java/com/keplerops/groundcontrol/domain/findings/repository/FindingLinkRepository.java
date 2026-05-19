package com.keplerops.groundcontrol.domain.findings.repository;

import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingLinkRepository extends JpaRepository<FindingLink, UUID> {

    List<FindingLink> findByFindingId(UUID findingId);

    @Query("SELECT l FROM FindingLink l JOIN FETCH l.finding WHERE l.finding.project.id = :projectId")
    List<FindingLink> findByProjectId(@Param("projectId") UUID projectId);

    Optional<FindingLink> findByIdAndFindingProjectId(UUID id, UUID projectId);

    boolean existsByFindingIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID findingId, FindingLinkTargetType targetType, String targetIdentifier, FindingLinkType linkType);

    boolean existsByFindingIdAndTargetTypeAndTargetEntityIdAndLinkType(
            UUID findingId, FindingLinkTargetType targetType, UUID targetEntityId, FindingLinkType linkType);

    @Query("SELECT l.finding.uid FROM FindingLink l WHERE l.targetType = :targetType"
            + " AND l.targetEntityId = :targetEntityId AND l.finding.project.id = :projectId")
    List<String> findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
            @Param("targetType") FindingLinkTargetType targetType,
            @Param("targetEntityId") UUID targetEntityId,
            @Param("projectId") UUID projectId);
}
