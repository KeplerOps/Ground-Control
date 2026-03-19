package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequirementRelationRepository extends JpaRepository<RequirementRelation, UUID> {

    List<RequirementRelation> findBySourceId(UUID sourceId);

    List<RequirementRelation> findByTargetId(UUID targetId);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.id = :sourceId")
    List<RequirementRelation> findBySourceIdWithEntities(@Param("sourceId") UUID sourceId);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.target.id = :targetId")
    List<RequirementRelation> findByTargetIdWithEntities(@Param("targetId") UUID targetId);

    boolean existsBySourceIdAndTargetIdAndRelationType(UUID sourceId, UUID targetId, RelationType relationType);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.relationType IN :types")
    List<RequirementRelation> findAllWithSourceAndTargetByRelationTypeIn(@Param("types") List<RelationType> types);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target")
    List<RequirementRelation> findAllWithSourceAndTarget();

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId AND r.relationType IN :types")
    List<RequirementRelation> findAllByProjectAndRelationTypeIn(
            @Param("projectId") UUID projectId, @Param("types") List<RelationType> types);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId")
    List<RequirementRelation> findAllWithSourceAndTargetByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId AND r.relationType IN :types"
            + " AND r.source.archivedAt IS NULL AND r.target.archivedAt IS NULL")
    List<RequirementRelation> findActiveByProjectAndRelationTypeIn(
            @Param("projectId") UUID projectId, @Param("types") List<RelationType> types);

    @Query("SELECT r FROM RequirementRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId"
            + " AND r.source.archivedAt IS NULL AND r.target.archivedAt IS NULL")
    List<RequirementRelation> findActiveWithSourceAndTargetByProjectId(@Param("projectId") UUID projectId);
}
