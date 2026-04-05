package com.keplerops.groundcontrol.domain.controls.repository;

import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlLinkRepository extends JpaRepository<ControlLink, UUID> {

    List<ControlLink> findByControlId(UUID controlId);

    List<ControlLink> findByControlIdAndTargetType(UUID controlId, ControlLinkTargetType targetType);

    Optional<ControlLink> findByIdAndControlProjectId(UUID id, UUID projectId);

    boolean existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID controlId,
            ControlLinkTargetType targetType,
            String targetIdentifier,
            com.keplerops.groundcontrol.domain.controls.state.ControlLinkType linkType);

    boolean existsByControlIdAndTargetTypeAndTargetEntityIdAndLinkType(
            UUID controlId,
            ControlLinkTargetType targetType,
            UUID targetEntityId,
            com.keplerops.groundcontrol.domain.controls.state.ControlLinkType linkType);
}
