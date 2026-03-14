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
}
