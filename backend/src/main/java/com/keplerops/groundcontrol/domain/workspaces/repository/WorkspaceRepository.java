package com.keplerops.groundcontrol.domain.workspaces.repository;

import com.keplerops.groundcontrol.domain.workspaces.model.Workspace;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    Optional<Workspace> findByIdentifier(String identifier);
    boolean existsByIdentifier(String identifier);
}
