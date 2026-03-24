package com.keplerops.groundcontrol.api.workspaces;

import com.keplerops.groundcontrol.domain.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@Valid @RequestBody WorkspaceRequest request) {
        return WorkspaceResponse.from(
                workspaceService.create(request.identifier(), request.name(), request.description()));
    }

    @GetMapping
    public List<WorkspaceResponse> list() {
        return workspaceService.listAll().stream().map(WorkspaceResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getById(@PathVariable UUID id) {
        return WorkspaceResponse.from(workspaceService.getById(id));
    }

    @PutMapping("/{id}")
    public WorkspaceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWorkspaceRequest request) {
        return WorkspaceResponse.from(workspaceService.update(id, request.name(), request.description()));
    }
}
