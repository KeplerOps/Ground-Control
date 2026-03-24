package com.keplerops.groundcontrol.api.executions;

import com.keplerops.groundcontrol.domain.executions.service.ExecutionService;
import com.keplerops.groundcontrol.domain.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ExecutionController {

    private final ExecutionService executionService;
    private final WorkspaceService workspaceService;

    public ExecutionController(ExecutionService executionService, WorkspaceService workspaceService) {
        this.executionService = executionService;
        this.workspaceService = workspaceService;
    }

    @PostMapping("/workflows/{workflowId}/execute")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionResponse execute(@PathVariable UUID workflowId,
                                      @RequestBody(required = false) ExecuteRequest request) {
        String inputs = request != null ? request.inputs() : null;
        return ExecutionResponse.from(
                executionService.createAndExecute(workflowId, "MANUAL", null, inputs));
    }

    @GetMapping("/workflows/{workflowId}/executions")
    public Page<ExecutionResponse> listByWorkflow(@PathVariable UUID workflowId, Pageable pageable) {
        return executionService.listByWorkflow(workflowId, pageable).map(ExecutionResponse::from);
    }

    @GetMapping("/executions")
    public Page<ExecutionResponse> listAll(@RequestParam(required = false) String workspace, Pageable pageable) {
        var workspaceId = workspaceService.resolveWorkspaceId(workspace);
        return executionService.listByWorkspace(workspaceId, pageable).map(ExecutionResponse::from);
    }

    @GetMapping("/executions/{id}")
    public ExecutionDetailResponse getById(@PathVariable UUID id) {
        var execution = executionService.getById(id);
        var tasks = executionService.getTaskExecutions(id);
        return ExecutionDetailResponse.from(execution, tasks);
    }

    @PostMapping("/executions/{id}/cancel")
    public ExecutionResponse cancel(@PathVariable UUID id) {
        return ExecutionResponse.from(executionService.cancel(id));
    }

    @PostMapping("/executions/{id}/retry")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionResponse retry(@PathVariable UUID id) {
        return ExecutionResponse.from(executionService.retry(id));
    }

    @GetMapping("/workflows/{workflowId}/executions/stats")
    public ExecutionService.ExecutionStats stats(@PathVariable UUID workflowId) {
        return executionService.getStats(workflowId);
    }
}
