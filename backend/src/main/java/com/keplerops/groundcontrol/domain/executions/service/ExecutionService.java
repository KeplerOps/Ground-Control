package com.keplerops.groundcontrol.domain.executions.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.executions.model.Execution;
import com.keplerops.groundcontrol.domain.executions.model.TaskExecution;
import com.keplerops.groundcontrol.domain.executions.repository.ExecutionRepository;
import com.keplerops.groundcontrol.domain.executions.repository.TaskExecutionRepository;
import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowNodeRepository;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowRepository;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExecutionService(
            ExecutionRepository executionRepository,
            TaskExecutionRepository taskExecutionRepository,
            WorkflowRepository workflowRepository,
            WorkflowNodeRepository nodeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.executionRepository = executionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.eventPublisher = eventPublisher;
    }

    public Execution createExecution(
            UUID workflowId, String triggerType, String triggerRef, String inputs) {
        var workflow =
                workflowRepository
                        .findById(workflowId)
                        .orElseThrow(() -> new NotFoundException("Workflow not found: " + workflowId));
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new DomainValidationException(
                    "Cannot execute a workflow that is not ACTIVE (current: "
                            + workflow.getStatus()
                            + ")");
        }
        var execution = new Execution(workflow);
        if (triggerType != null) execution.setTriggerType(triggerType);
        if (triggerRef != null) execution.setTriggerRef(triggerRef);
        if (inputs != null) execution.setInputs(inputs);
        execution = executionRepository.save(execution);

        // Create task executions for each node
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowId(workflowId);
        for (WorkflowNode node : nodes) {
            var taskExecution = new TaskExecution(execution, node);
            taskExecutionRepository.save(taskExecution);
        }

        return execution;
    }

    public Execution createAndExecute(UUID workflowId, String triggerType, String triggerRef, String inputs) {
        var execution = createExecution(workflowId, triggerType, triggerRef, inputs);
        eventPublisher.publishEvent(new ExecutionRequestedEvent(execution.getId()));
        return execution;
    }

    @Transactional(readOnly = true)
    public Execution getById(UUID id) {
        return executionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Execution> listByWorkflow(UUID workflowId, Pageable pageable) {
        return executionRepository.findByWorkflowId(workflowId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Execution> listByWorkspace(UUID workspaceId, Pageable pageable) {
        return executionRepository.findByWorkflowWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public List<TaskExecution> getTaskExecutions(UUID executionId) {
        return taskExecutionRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
    }

    public Execution cancel(UUID executionId) {
        var execution = getById(executionId);
        execution.cancel();
        // Cancel all pending/running tasks
        var tasks = taskExecutionRepository.findByExecutionId(executionId);
        for (var task : tasks) {
            if (!task.getStatus().isTerminal()) {
                task.transitionStatus(ExecutionStatus.CANCELLED);
                taskExecutionRepository.save(task);
            }
        }
        return executionRepository.save(execution);
    }

    public Execution retry(UUID executionId) {
        var original = getById(executionId);
        if (original.getStatus() != ExecutionStatus.FAILED
                && original.getStatus() != ExecutionStatus.TIMED_OUT) {
            throw new DomainValidationException(
                    "Can only retry failed or timed-out executions");
        }
        return createExecution(
                original.getWorkflow().getId(),
                "RETRY",
                executionId.toString(),
                original.getInputs());
    }

    public TaskExecution updateTaskExecution(
            UUID taskExecutionId, ExecutionStatus status, String outputs, String logs, String error) {
        var task =
                taskExecutionRepository
                        .findById(taskExecutionId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        "Task execution not found: " + taskExecutionId));
        if (status != null) task.transitionStatus(status);
        if (logs != null) task.appendLog(logs);
        return taskExecutionRepository.save(task);
    }

    @Transactional(readOnly = true)
    public ExecutionStats getStats(UUID workflowId) {
        long total = executionRepository.countByWorkflowId(workflowId);
        long success =
                executionRepository.countByWorkflowIdAndStatus(workflowId, ExecutionStatus.SUCCESS);
        long failed =
                executionRepository.countByWorkflowIdAndStatus(workflowId, ExecutionStatus.FAILED);
        long running =
                executionRepository.countByWorkflowIdAndStatus(workflowId, ExecutionStatus.RUNNING);
        return new ExecutionStats(total, success, failed, running);
    }

    public record ExecutionStats(long total, long success, long failed, long running) {}
}
