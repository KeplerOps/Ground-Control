package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequirementRepository extends JpaRepository<Requirement, UUID>, JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByUid(String uid);

    boolean existsByUid(String uid);

    Optional<Requirement> findByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId AND UPPER(r.uid) = UPPER(:uid)")
    Optional<Requirement> findByProjectIdAndUidIgnoreCase(@Param("projectId") UUID projectId, @Param("uid") String uid);

    @Query(
            "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Requirement r WHERE r.project.id = :projectId AND UPPER(r.uid) = UPPER(:uid)")
    boolean existsByProjectIdAndUidIgnoreCase(@Param("projectId") UUID projectId, @Param("uid") String uid);

    List<Requirement> findByProjectId(UUID projectId);

    List<Requirement> findByProjectIdAndArchivedAtIsNull(UUID projectId);

    long countByProjectIdAndArchivedAtIsNull(UUID projectId);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId AND r.archivedAt IS NULL"
            + " AND NOT EXISTS (SELECT rel FROM RequirementRelation rel WHERE rel.source = r OR rel.target = r)"
            + " AND NOT EXISTS (SELECT l FROM TraceabilityLink l WHERE l.requirement = r)")
    List<Requirement> findOrphansByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId AND r.archivedAt IS NULL"
            + " AND NOT EXISTS (SELECT l FROM TraceabilityLink l WHERE l.requirement = r AND l.linkType = :linkType)")
    List<Requirement> findCoverageGapsByProjectIdAndLinkType(
            @Param("projectId") UUID projectId, @Param("linkType") LinkType linkType);
}
