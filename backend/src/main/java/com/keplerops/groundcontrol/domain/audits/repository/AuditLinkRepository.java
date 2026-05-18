package com.keplerops.groundcontrol.domain.audits.repository;

import com.keplerops.groundcontrol.domain.audits.model.AuditLink;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLinkRepository extends JpaRepository<AuditLink, UUID> {

    List<AuditLink> findByAuditId(UUID auditId);

    @Query("SELECT l FROM AuditLink l JOIN FETCH l.audit WHERE l.audit.project.id = :projectId")
    List<AuditLink> findByProjectId(@Param("projectId") UUID projectId);

    Optional<AuditLink> findByIdAndAuditProjectId(UUID id, UUID projectId);

    boolean existsByAuditIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID auditId, AuditLinkTargetType targetType, String targetIdentifier, AuditLinkType linkType);

    boolean existsByAuditIdAndTargetTypeAndTargetEntityIdAndLinkType(
            UUID auditId, AuditLinkTargetType targetType, UUID targetEntityId, AuditLinkType linkType);

    @Query("SELECT l.audit.uid FROM AuditLink l WHERE l.targetType = :targetType"
            + " AND l.targetEntityId = :targetEntityId AND l.audit.project.id = :projectId")
    List<String> findAuditUidsByTargetTypeAndTargetEntityIdAndProjectId(
            @Param("targetType") AuditLinkTargetType targetType,
            @Param("targetEntityId") UUID targetEntityId,
            @Param("projectId") UUID projectId);
}
