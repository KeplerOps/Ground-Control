package com.keplerops.groundcontrol.domain.credentials.service;

import com.keplerops.groundcontrol.domain.credentials.model.Credential;
import com.keplerops.groundcontrol.domain.credentials.repository.CredentialRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.workspaces.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final WorkspaceRepository workspaceRepository;

    public CredentialService(
            CredentialRepository credentialRepository, WorkspaceRepository workspaceRepository) {
        this.credentialRepository = credentialRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public Credential create(
            UUID workspaceId, String name, String credentialType, String encryptedData) {
        var workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceId));
        if (credentialRepository.existsByWorkspaceIdAndName(workspaceId, name)) {
            throw new ConflictException(
                    "Credential with name '" + name + "' already exists in workspace");
        }
        var credential = new Credential(workspace, name, credentialType);
        if (encryptedData != null) credential.setEncryptedData(encryptedData);
        return credentialRepository.save(credential);
    }

    @Transactional(readOnly = true)
    public Credential getById(UUID id) {
        return credentialRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Credential not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Credential> listByWorkspace(UUID workspaceId) {
        return credentialRepository.findByWorkspaceId(workspaceId);
    }

    public Credential update(UUID id, String name, String encryptedData) {
        var credential = getById(id);
        if (name != null) credential.setName(name);
        if (encryptedData != null) credential.setEncryptedData(encryptedData);
        return credentialRepository.save(credential);
    }

    public void delete(UUID id) {
        var credential = getById(id);
        credentialRepository.delete(credential);
    }
}
