package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementRelationRepository extends JpaRepository<RequirementRelation, UUID> {

    List<RequirementRelation> findBySourceId(UUID sourceId);

    List<RequirementRelation> findByTargetId(UUID targetId);
}
