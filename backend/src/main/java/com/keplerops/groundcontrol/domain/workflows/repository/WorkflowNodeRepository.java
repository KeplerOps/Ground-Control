package com.keplerops.groundcontrol.domain.workflows.repository;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, UUID> {
    List<WorkflowNode> findByWorkflowId(UUID workflowId);
    Optional<WorkflowNode> findByWorkflowIdAndName(UUID workflowId, String name);
    boolean existsByWorkflowIdAndName(UUID workflowId, String name);
    void deleteByWorkflowId(UUID workflowId);
}
