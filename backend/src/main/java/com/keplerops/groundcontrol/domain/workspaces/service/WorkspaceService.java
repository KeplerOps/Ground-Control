package com.keplerops.groundcontrol.domain.workspaces.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.workspaces.model.Workspace;
import com.keplerops.groundcontrol.domain.workspaces.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Workspace create(String identifier, String name, String description) {
        if (workspaceRepository.existsByIdentifier(identifier)) {
            throw new ConflictException("Workspace with identifier '" + identifier + "' already exists");
        }
        var workspace = new Workspace(identifier, name);
        if (description != null) {
            workspace.setDescription(description);
        }
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace getById(UUID id) {
        return workspaceRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Workspace not found: " + id));
    }

    @Transactional(readOnly = true)
    public Workspace getByIdentifier(String identifier) {
        return workspaceRepository
                .findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException("Workspace not found: " + identifier));
    }

    @Transactional(readOnly = true)
    public List<Workspace> listAll() {
        return workspaceRepository.findAll();
    }

    public Workspace update(UUID id, String name, String description) {
        var workspace = getById(id);
        if (name != null) {
            workspace.setName(name);
        }
        if (description != null) {
            workspace.setDescription(description);
        }
        return workspaceRepository.save(workspace);
    }

    public UUID resolveWorkspaceId(String workspaceParam) {
        if (workspaceParam == null || workspaceParam.isBlank()) {
            var all = workspaceRepository.findAll();
            if (all.isEmpty()) {
                var defaultWs = create("default", "Default Workspace", "Auto-created default workspace");
                return defaultWs.getId();
            }
            return all.getFirst().getId();
        }
        try {
            return UUID.fromString(workspaceParam);
        } catch (IllegalArgumentException e) {
            return getByIdentifier(workspaceParam).getId();
        }
    }
}
