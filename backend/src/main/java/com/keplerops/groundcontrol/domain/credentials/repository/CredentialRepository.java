package com.keplerops.groundcontrol.domain.credentials.repository;

import com.keplerops.groundcontrol.domain.credentials.model.Credential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    List<Credential> findByWorkspaceId(UUID workspaceId);
    Optional<Credential> findByWorkspaceIdAndName(UUID workspaceId, String name);
    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
}
