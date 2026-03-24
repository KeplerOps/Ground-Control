package com.keplerops.groundcontrol.domain.workflows.repository;

import com.keplerops.groundcontrol.domain.workflows.model.Workflow;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    Page<Workflow> findByWorkspaceId(UUID workspaceId, Pageable pageable);
    List<Workflow> findByWorkspaceIdAndStatus(UUID workspaceId, WorkflowStatus status);
    List<Workflow> findByStatus(WorkflowStatus status);
}
