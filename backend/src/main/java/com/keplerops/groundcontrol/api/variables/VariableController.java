package com.keplerops.groundcontrol.api.variables;

import com.keplerops.groundcontrol.domain.variables.service.VariableService;
import com.keplerops.groundcontrol.domain.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/variables")
public class VariableController {

    private final VariableService variableService;
    private final WorkspaceService workspaceService;

    public VariableController(VariableService variableService, WorkspaceService workspaceService) {
        this.variableService = variableService;
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VariableResponse create(@Valid @RequestBody VariableRequest request,
                                    @RequestParam(required = false) String workspace) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return VariableResponse.from(
                variableService.create(workspaceId, request.key(), request.value(),
                        request.description(), request.secret() != null && request.secret()));
    }

    @GetMapping
    public List<VariableResponse> list(@RequestParam(required = false) String workspace) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return variableService.listByWorkspace(workspaceId).stream().map(VariableResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VariableResponse getById(@PathVariable UUID id) {
        return VariableResponse.from(variableService.getById(id));
    }

    @PutMapping("/{id}")
    public VariableResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVariableRequest request) {
        return VariableResponse.from(variableService.update(id, request.value(), request.description(), request.secret()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        variableService.delete(id);
    }
}
