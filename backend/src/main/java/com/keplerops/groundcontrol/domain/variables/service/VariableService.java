package com.keplerops.groundcontrol.domain.variables.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.variables.model.Variable;
import com.keplerops.groundcontrol.domain.variables.repository.VariableRepository;
import com.keplerops.groundcontrol.domain.workspaces.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VariableService {

    private final VariableRepository variableRepository;
    private final WorkspaceRepository workspaceRepository;

    public VariableService(
            VariableRepository variableRepository, WorkspaceRepository workspaceRepository) {
        this.variableRepository = variableRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public Variable create(
            UUID workspaceId, String key, String value, String description, boolean secret) {
        var workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceId));
        if (variableRepository.existsByWorkspaceIdAndKey(workspaceId, key)) {
            throw new ConflictException(
                    "Variable with key '" + key + "' already exists in workspace");
        }
        var variable = new Variable(workspace, key);
        if (value != null) variable.setValue(value);
        if (description != null) variable.setDescription(description);
        variable.setSecret(secret);
        return variableRepository.save(variable);
    }

    @Transactional(readOnly = true)
    public Variable getById(UUID id) {
        return variableRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Variable not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Variable> listByWorkspace(UUID workspaceId) {
        return variableRepository.findByWorkspaceId(workspaceId);
    }

    public Variable update(UUID id, String value, String description, Boolean secret) {
        var variable = getById(id);
        if (value != null) variable.setValue(value);
        if (description != null) variable.setDescription(description);
        if (secret != null) variable.setSecret(secret);
        return variableRepository.save(variable);
    }

    public void delete(UUID id) {
        var variable = getById(id);
        variableRepository.delete(variable);
    }
}
