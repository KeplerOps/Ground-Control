package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.executions.model.TaskExecution;
import com.keplerops.groundcontrol.domain.executions.repository.ExecutionRepository;
import com.keplerops.groundcontrol.domain.executions.repository.TaskExecutionRepository;
import com.keplerops.groundcontrol.domain.executions.service.ExecutionRequestedEvent;
import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import com.keplerops.groundcontrol.domain.workflows.service.WorkflowService;
import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates workflow execution by walking the DAG in topological order.
 * Listens for ExecutionRequestedEvent from the domain layer.
 */
@Component
public class WorkflowExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionEngine.class);

    private final WorkflowService workflowService;
    private final ExecutionRepository executionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final Map<NodeType, TaskExecutor> executors;
    private final ExecutorService threadPool;

    public WorkflowExecutionEngine(
            WorkflowService workflowService,
            ExecutionRepository executionRepository,
            TaskExecutionRepository taskExecutionRepository,
            ShellTaskExecutor shellExecutor,
            HttpTaskExecutor httpExecutor,
            DockerTaskExecutor dockerExecutor) {
        this.workflowService = workflowService;
        this.executionRepository = executionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.executors = new HashMap<>();
        this.executors.put(NodeType.SHELL, shellExecutor);
        this.executors.put(NodeType.HTTP, httpExecutor);
        this.executors.put(NodeType.DOCKER, dockerExecutor);
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @EventListener
    public void onExecutionRequested(ExecutionRequestedEvent event) {
        executeAsync(event.executionId());
    }

    public void executeAsync(UUID executionId) {
        threadPool.submit(() -> {
            try {
                executeWorkflow(executionId);
            } catch (Exception e) {
                log.error("Workflow execution {} failed unexpectedly", executionId, e);
                failExecution(executionId, e.getMessage());
            }
        });
    }

    @Transactional
    public void executeWorkflow(UUID executionId) {
        var execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            log.error("Execution not found: {}", executionId);
            return;
        }

        execution.markStarted();
        executionRepository.save(execution);

        var workflowId = execution.getWorkflow().getId();
        var orderedNodes = workflowService.getTopologicalOrder(workflowId);
        var taskExecutions = taskExecutionRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);

        Map<UUID, TaskExecution> taskMap = new HashMap<>();
        for (var te : taskExecutions) {
            if (te.getNode() != null) {
                taskMap.put(te.getNode().getId(), te);
            }
        }

        boolean allSuccess = true;
        for (WorkflowNode node : orderedNodes) {
            TaskExecution taskExec = taskMap.get(node.getId());
            if (taskExec == null) continue;

            if (node.getNodeType() == NodeType.NOOP
                    || node.getNodeType() == NodeType.DELAY
                    || node.getNodeType() == NodeType.CONDITIONAL
                    || node.getNodeType() == NodeType.TRANSFORM
                    || node.getNodeType() == NodeType.NOTIFICATION
                    || node.getNodeType() == NodeType.SUB_WORKFLOW
                    || node.getNodeType() == NodeType.SCRIPT) {
                taskExec.markSuccess("{}");
                taskExecutionRepository.save(taskExec);
                continue;
            }

            TaskExecutor executor = executors.get(node.getNodeType());
            if (executor == null) {
                taskExec.markFailed("No executor for node type: " + node.getNodeType());
                taskExecutionRepository.save(taskExec);
                allSuccess = false;
                break;
            }

            taskExec.markRunning();
            taskExecutionRepository.save(taskExec);

            var result = executor.execute(node, taskExec.getInputs());
            taskExec.appendLog(result.logs());

            if (result.success()) {
                taskExec.markSuccess(result.outputs());
            } else {
                taskExec.markFailed(result.error());
                allSuccess = false;
                break;
            }
            taskExecutionRepository.save(taskExec);
        }

        execution = executionRepository.findById(executionId).orElse(null);
        if (execution != null) {
            if (allSuccess) {
                execution.markSuccess("{}");
            } else {
                execution.markFailed("One or more tasks failed");
            }
            executionRepository.save(execution);
        }
    }

    @Transactional
    void failExecution(UUID executionId, String error) {
        var execution = executionRepository.findById(executionId).orElse(null);
        if (execution != null && !execution.getStatus().isTerminal()) {
            execution.markFailed(error);
            executionRepository.save(execution);
        }
    }
}
