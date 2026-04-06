package com.keplerops.groundcontrol.domain.controls.repository;

import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ControlLinkRepository extends JpaRepository<ControlLink, UUID> {

    @Query("SELECT l FROM ControlLink l JOIN FETCH l.control WHERE l.control.project.id = :projectId")
    List<ControlLink> findByProjectId(@Param("projectId") UUID projectId);

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
