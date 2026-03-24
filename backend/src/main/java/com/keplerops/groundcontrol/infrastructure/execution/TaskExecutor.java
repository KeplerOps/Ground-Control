package com.keplerops.groundcontrol.infrastructure.execution;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;

/**
 * Interface for executing a single workflow node/task.
 */
public interface TaskExecutor {

    TaskResult execute(WorkflowNode node, String inputs);

    record TaskResult(boolean success, String outputs, String logs, String error) {
        public static TaskResult success(String outputs, String logs) {
            return new TaskResult(true, outputs, logs, "");
        }

        public static TaskResult failure(String error, String logs) {
            return new TaskResult(false, "{}", logs, error);
        }
    }
}
