package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementEmbedding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementEmbeddingRepository extends JpaRepository<RequirementEmbedding, UUID> {

    Optional<RequirementEmbedding> findByRequirementId(UUID requirementId);

    List<RequirementEmbedding> findByRequirementProjectId(UUID projectId);

    void deleteByRequirementId(UUID requirementId);

    boolean existsByRequirementId(UUID requirementId);
}
