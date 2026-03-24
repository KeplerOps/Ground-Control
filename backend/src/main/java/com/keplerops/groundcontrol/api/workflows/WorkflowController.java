package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.service.WorkflowService;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import com.keplerops.groundcontrol.domain.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkspaceService workspaceService;

    public WorkflowController(WorkflowService workflowService, WorkspaceService workspaceService) {
        this.workflowService = workflowService;
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse create(@Valid @RequestBody WorkflowRequest request,
                                    @RequestParam(required = false) String workspace) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return WorkflowResponse.from(workflowService.create(
                workspaceId, request.name(), request.description(), request.tags(),
                request.timeoutSeconds(), request.maxRetries(), request.retryBackoffMs()));
    }

    @GetMapping
    public Page<WorkflowResponse> list(@RequestParam(required = false) String workspace, Pageable pageable) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return workflowService.list(workspaceId, pageable).map(WorkflowResponse::from);
    }

    @GetMapping("/{id}")
    public WorkflowResponse getById(@PathVariable UUID id) {
        return WorkflowResponse.from(workflowService.getById(id));
    }

    @PutMapping("/{id}")
    public WorkflowResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWorkflowRequest request) {
        return WorkflowResponse.from(workflowService.update(
                id, request.name(), request.description(), request.tags(),
                request.timeoutSeconds(), request.maxRetries(), request.retryBackoffMs()));
    }

    @PostMapping("/{id}/transition")
    public WorkflowResponse transitionStatus(@PathVariable UUID id,
                                              @Valid @RequestBody StatusTransitionRequest request) {
        return WorkflowResponse.from(workflowService.transitionStatus(id, request.status()));
    }

    @PostMapping("/{id}/publish")
    public WorkflowResponse publish(@PathVariable UUID id) {
        return WorkflowResponse.from(workflowService.publish(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        workflowService.delete(id);
    }

    // --- Node operations ---

    @PostMapping("/{id}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse addNode(@PathVariable UUID id, @Valid @RequestBody NodeRequest request) {
        return NodeResponse.from(workflowService.addNode(
                id, request.name(), request.nodeType(), request.label(), request.config(),
                request.positionX(), request.positionY(), request.timeoutSeconds(), request.retryPolicy()));
    }

    @GetMapping("/{id}/nodes")
    public List<NodeResponse> getNodes(@PathVariable UUID id) {
        return workflowService.getNodes(id).stream().map(NodeResponse::from).toList();
    }

    @PutMapping("/{id}/nodes/{nodeId}")
    public NodeResponse updateNode(@PathVariable UUID id, @PathVariable UUID nodeId,
                                    @Valid @RequestBody UpdateNodeRequest request) {
        return NodeResponse.from(workflowService.updateNode(
                id, nodeId, request.name(), request.label(), request.nodeType(), request.config(),
                request.positionX(), request.positionY(), request.timeoutSeconds(), request.retryPolicy()));
    }

    @DeleteMapping("/{id}/nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable UUID id, @PathVariable UUID nodeId) {
        workflowService.deleteNode(id, nodeId);
    }

    // --- Edge operations ---

    @PostMapping("/{id}/edges")
    @ResponseStatus(HttpStatus.CREATED)
    public EdgeResponse addEdge(@PathVariable UUID id, @Valid @RequestBody EdgeRequest request) {
        return EdgeResponse.from(workflowService.addEdge(
                id, request.sourceNodeId(), request.targetNodeId(), request.conditionExpr(), request.label()));
    }

    @GetMapping("/{id}/edges")
    public List<EdgeResponse> getEdges(@PathVariable UUID id) {
        return workflowService.getEdges(id).stream().map(EdgeResponse::from).toList();
    }

    @DeleteMapping("/{id}/edges/{edgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEdge(@PathVariable UUID id, @PathVariable UUID edgeId) {
        workflowService.deleteEdge(id, edgeId);
    }

    // --- Validation ---

    @PostMapping("/{id}/validate")
    public ValidationResponse validate(@PathVariable UUID id) {
        try {
            workflowService.validateDag(id);
            return new ValidationResponse(true, "Workflow DAG is valid");
        } catch (Exception e) {
            return new ValidationResponse(false, e.getMessage());
        }
    }
}
