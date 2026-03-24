package com.keplerops.groundcontrol.domain.executions.repository;

import com.keplerops.groundcontrol.domain.executions.model.Execution;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
    Page<Execution> findByWorkflowId(UUID workflowId, Pageable pageable);
    Page<Execution> findByWorkflowWorkspaceId(UUID workspaceId, Pageable pageable);
    List<Execution> findByStatus(ExecutionStatus status);
    long countByWorkflowId(UUID workflowId);
    long countByWorkflowIdAndStatus(UUID workflowId, ExecutionStatus status);
}
