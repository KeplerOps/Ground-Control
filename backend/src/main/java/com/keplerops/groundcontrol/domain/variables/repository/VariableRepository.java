package com.keplerops.groundcontrol.domain.variables.repository;

import com.keplerops.groundcontrol.domain.variables.model.Variable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableRepository extends JpaRepository<Variable, UUID> {
    List<Variable> findByWorkspaceId(UUID workspaceId);
    Optional<Variable> findByWorkspaceIdAndKey(UUID workspaceId, String key);
    boolean existsByWorkspaceIdAndKey(UUID workspaceId, String key);
}
