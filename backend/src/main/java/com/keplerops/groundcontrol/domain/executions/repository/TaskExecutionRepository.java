package com.keplerops.groundcontrol.domain.executions.repository;

import com.keplerops.groundcontrol.domain.executions.model.TaskExecution;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, UUID> {
    List<TaskExecution> findByExecutionId(UUID executionId);
    List<TaskExecution> findByExecutionIdAndStatus(UUID executionId, ExecutionStatus status);
    List<TaskExecution> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
