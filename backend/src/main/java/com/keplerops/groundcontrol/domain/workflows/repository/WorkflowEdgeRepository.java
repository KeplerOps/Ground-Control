package com.keplerops.groundcontrol.domain.workflows.repository;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowEdge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdge, UUID> {
    List<WorkflowEdge> findByWorkflowId(UUID workflowId);
    List<WorkflowEdge> findBySourceNodeId(UUID sourceNodeId);
    List<WorkflowEdge> findByTargetNodeId(UUID targetNodeId);
    boolean existsByWorkflowIdAndSourceNodeIdAndTargetNodeId(UUID workflowId, UUID sourceNodeId, UUID targetNodeId);
    void deleteByWorkflowId(UUID workflowId);

    @Query("SELECT e FROM WorkflowEdge e WHERE e.sourceNode.id = :nodeId OR e.targetNode.id = :nodeId")
    List<WorkflowEdge> findByNodeId(UUID nodeId);
}
