package com.keplerops.groundcontrol.api.credentials;

import com.keplerops.groundcontrol.domain.credentials.service.CredentialService;
import com.keplerops.groundcontrol.domain.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {

    private final CredentialService credentialService;
    private final WorkspaceService workspaceService;

    public CredentialController(CredentialService credentialService, WorkspaceService workspaceService) {
        this.credentialService = credentialService;
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponse create(@Valid @RequestBody CredentialRequest request,
                                      @RequestParam(required = false) String workspace) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return CredentialResponse.from(
                credentialService.create(workspaceId, request.name(), request.credentialType(), request.data()));
    }

    @GetMapping
    public List<CredentialResponse> list(@RequestParam(required = false) String workspace) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return credentialService.listByWorkspace(workspaceId).stream().map(CredentialResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CredentialResponse getById(@PathVariable UUID id) {
        return CredentialResponse.from(credentialService.getById(id));
    }

    @PutMapping("/{id}")
    public CredentialResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCredentialRequest request) {
        return CredentialResponse.from(credentialService.update(id, request.name(), request.data()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        credentialService.delete(id);
    }
}
