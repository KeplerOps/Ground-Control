package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRelationRepository extends JpaRepository<AssetRelation, UUID> {

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target WHERE r.id = :id")
    java.util.Optional<AssetRelation> findByIdWithEntities(@Param("id") UUID id);

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.id = :id AND r.source.project.id = :projectId AND r.target.project.id = :projectId")
    java.util.Optional<AssetRelation> findByIdWithEntitiesAndProjectId(
            @Param("id") UUID id, @Param("projectId") UUID projectId);

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target" + " WHERE r.source.id = :sourceId")
    List<AssetRelation> findBySourceIdWithEntities(@Param("sourceId") UUID sourceId);

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target" + " WHERE r.target.id = :targetId")
    List<AssetRelation> findByTargetIdWithEntities(@Param("targetId") UUID targetId);

    boolean existsBySourceIdAndTargetIdAndRelationType(UUID sourceId, UUID targetId, AssetRelationType relationType);

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId"
            + " AND r.source.archivedAt IS NULL AND r.target.archivedAt IS NULL")
    List<AssetRelation> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT r FROM AssetRelation r JOIN FETCH r.source JOIN FETCH r.target"
            + " WHERE r.source.project.id = :projectId AND r.relationType IN :types"
            + " AND r.source.archivedAt IS NULL AND r.target.archivedAt IS NULL")
    List<AssetRelation> findActiveByProjectAndRelationTypeIn(
            @Param("projectId") UUID projectId, @Param("types") List<AssetRelationType> types);
}
