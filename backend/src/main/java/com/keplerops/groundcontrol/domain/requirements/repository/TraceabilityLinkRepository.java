package com.keplerops.groundcontrol.domain.requirements.repository;

import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceabilityLinkRepository extends JpaRepository<TraceabilityLink, UUID> {

    List<TraceabilityLink> findByRequirementId(UUID requirementId);

    boolean existsByRequirementIdAndArtifactTypeAndArtifactIdentifierAndLinkType(
            UUID requirementId, ArtifactType artifactType, String artifactIdentifier, LinkType linkType);
}
