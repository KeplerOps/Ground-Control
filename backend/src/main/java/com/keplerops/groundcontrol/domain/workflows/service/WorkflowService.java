package com.keplerops.groundcontrol.domain.workflows.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.workflows.model.Workflow;
import com.keplerops.groundcontrol.domain.workflows.model.WorkflowEdge;
import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowEdgeRepository;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowNodeRepository;
import com.keplerops.groundcontrol.domain.workflows.repository.WorkflowRepository;
import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import com.keplerops.groundcontrol.domain.workspaces.model.Workspace;
import com.keplerops.groundcontrol.domain.workspaces.repository.WorkspaceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkflowService(
            WorkflowRepository workflowRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkspaceRepository workspaceRepository) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public Workflow create(
            UUID workspaceId,
            String name,
            String description,
            String tags,
            Integer timeoutSeconds,
            Integer maxRetries,
            Integer retryBackoffMs) {
        Workspace workspace =
                workspaceRepository
                        .findById(workspaceId)
                        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceId));
        var workflow = new Workflow(workspace, name);
        if (description != null) workflow.setDescription(description);
        if (tags != null) workflow.setTags(tags);
        if (timeoutSeconds != null) workflow.setTimeoutSeconds(timeoutSeconds);
        if (maxRetries != null) workflow.setMaxRetries(maxRetries);
        if (retryBackoffMs != null) workflow.setRetryBackoffMs(retryBackoffMs);
        return workflowRepository.save(workflow);
    }

    @Transactional(readOnly = true)
    public Workflow getById(UUID id) {
        return workflowRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Workflow not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Workflow> list(UUID workspaceId, Pageable pageable) {
        return workflowRepository.findByWorkspaceId(workspaceId, pageable);
    }

    public Workflow update(
            UUID id,
            String name,
            String description,
            String tags,
            Integer timeoutSeconds,
            Integer maxRetries,
            Integer retryBackoffMs) {
        var workflow = getById(id);
        if (name != null) workflow.setName(name);
        if (description != null) workflow.setDescription(description);
        if (tags != null) workflow.setTags(tags);
        if (timeoutSeconds != null) workflow.setTimeoutSeconds(timeoutSeconds);
        if (maxRetries != null) workflow.setMaxRetries(maxRetries);
        if (retryBackoffMs != null) workflow.setRetryBackoffMs(retryBackoffMs);
        return workflowRepository.save(workflow);
    }

    public Workflow transitionStatus(UUID id, WorkflowStatus newStatus) {
        var workflow = getById(id);
        workflow.transitionStatus(newStatus);
        return workflowRepository.save(workflow);
    }

    public Workflow publish(UUID id) {
        var workflow = getById(id);
        var nodes = nodeRepository.findByWorkflowId(id);
        if (nodes.isEmpty()) {
            throw new DomainValidationException("Cannot publish a workflow with no nodes");
        }
        validateDag(id);
        workflow.publish();
        return workflowRepository.save(workflow);
    }

    public void delete(UUID id) {
        var workflow = getById(id);
        if (workflow.getStatus() == WorkflowStatus.ACTIVE) {
            throw new DomainValidationException(
                    "Cannot delete an active workflow. Pause or archive it first.");
        }
        workflowRepository.delete(workflow);
    }

    // --- Node operations ---

    public WorkflowNode addNode(
            UUID workflowId,
            String name,
            NodeType nodeType,
            String label,
            String config,
            Integer positionX,
            Integer positionY,
            Integer timeoutSeconds,
            String retryPolicy) {
        var workflow = getById(workflowId);
        if (nodeRepository.existsByWorkflowIdAndName(workflowId, name)) {
            throw new ConflictException("Node with name '" + name + "' already exists in workflow");
        }
        var node = new WorkflowNode(workflow, name, nodeType);
        if (label != null) node.setLabel(label);
        if (config != null) node.setConfig(config);
        if (positionX != null) node.setPositionX(positionX);
        if (positionY != null) node.setPositionY(positionY);
        if (timeoutSeconds != null) node.setTimeoutSeconds(timeoutSeconds);
        if (retryPolicy != null) node.setRetryPolicy(retryPolicy);
        return nodeRepository.save(node);
    }

    public WorkflowNode updateNode(
            UUID workflowId,
            UUID nodeId,
            String name,
            String label,
            NodeType nodeType,
            String config,
            Integer positionX,
            Integer positionY,
            Integer timeoutSeconds,
            String retryPolicy) {
        var node =
                nodeRepository
                        .findById(nodeId)
                        .orElseThrow(() -> new NotFoundException("Node not found: " + nodeId));
        if (!node.getWorkflow().getId().equals(workflowId)) {
            throw new NotFoundException(
                    "Node " + nodeId + " does not belong to workflow " + workflowId);
        }
        if (name != null && !name.equals(node.getName())) {
            if (nodeRepository.existsByWorkflowIdAndName(workflowId, name)) {
                throw new ConflictException(
                        "Node with name '" + name + "' already exists in workflow");
            }
            node.setName(name);
        }
        if (label != null) node.setLabel(label);
        if (nodeType != null) node.setNodeType(nodeType);
        if (config != null) node.setConfig(config);
        if (positionX != null) node.setPositionX(positionX);
        if (positionY != null) node.setPositionY(positionY);
        if (timeoutSeconds != null) node.setTimeoutSeconds(timeoutSeconds);
        if (retryPolicy != null) node.setRetryPolicy(retryPolicy);
        return nodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    public List<WorkflowNode> getNodes(UUID workflowId) {
        getById(workflowId); // verify exists
        return nodeRepository.findByWorkflowId(workflowId);
    }

    public void deleteNode(UUID workflowId, UUID nodeId) {
        var node =
                nodeRepository
                        .findById(nodeId)
                        .orElseThrow(() -> new NotFoundException("Node not found: " + nodeId));
        if (!node.getWorkflow().getId().equals(workflowId)) {
            throw new NotFoundException(
                    "Node " + nodeId + " does not belong to workflow " + workflowId);
        }
        // Delete connected edges first
        var connectedEdges = edgeRepository.findByNodeId(nodeId);
        edgeRepository.deleteAll(connectedEdges);
        nodeRepository.delete(node);
    }

    // --- Edge operations ---

    public WorkflowEdge addEdge(
            UUID workflowId,
            UUID sourceNodeId,
            UUID targetNodeId,
            String conditionExpr,
            String label) {
        var workflow = getById(workflowId);
        var source =
                nodeRepository
                        .findById(sourceNodeId)
                        .orElseThrow(
                                () -> new NotFoundException("Source node not found: " + sourceNodeId));
        var target =
                nodeRepository
                        .findById(targetNodeId)
                        .orElseThrow(
                                () -> new NotFoundException("Target node not found: " + targetNodeId));
        if (!source.getWorkflow().getId().equals(workflowId)
                || !target.getWorkflow().getId().equals(workflowId)) {
            throw new DomainValidationException("Both nodes must belong to the same workflow");
        }
        if (edgeRepository.existsByWorkflowIdAndSourceNodeIdAndTargetNodeId(
                workflowId, sourceNodeId, targetNodeId)) {
            throw new ConflictException("Edge already exists between these nodes");
        }
        var edge = new WorkflowEdge(workflow, source, target);
        if (conditionExpr != null) edge.setConditionExpr(conditionExpr);
        if (label != null) edge.setLabel(label);
        return edgeRepository.save(edge);
    }

    @Transactional(readOnly = true)
    public List<WorkflowEdge> getEdges(UUID workflowId) {
        getById(workflowId);
        return edgeRepository.findByWorkflowId(workflowId);
    }

    public void deleteEdge(UUID workflowId, UUID edgeId) {
        var edge =
                edgeRepository
                        .findById(edgeId)
                        .orElseThrow(() -> new NotFoundException("Edge not found: " + edgeId));
        if (!edge.getWorkflow().getId().equals(workflowId)) {
            throw new NotFoundException(
                    "Edge " + edgeId + " does not belong to workflow " + workflowId);
        }
        edgeRepository.delete(edge);
    }

    // --- DAG validation ---

    @Transactional(readOnly = true)
    public void validateDag(UUID workflowId) {
        var nodes = nodeRepository.findByWorkflowId(workflowId);
        var edges = edgeRepository.findByWorkflowId(workflowId);

        // Build adjacency list
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        Map<UUID, Integer> inDegree = new HashMap<>();
        for (var node : nodes) {
            adjacency.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        for (var edge : edges) {
            adjacency.get(edge.getSourceNode().getId()).add(edge.getTargetNode().getId());
            inDegree.merge(edge.getTargetNode().getId(), 1, Integer::sum);
        }

        // Kahn's algorithm for cycle detection
        var queue = new LinkedList<UUID>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        int visited = 0;
        while (!queue.isEmpty()) {
            var current = queue.poll();
            visited++;
            for (var neighbor : adjacency.getOrDefault(current, List.of())) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }
        if (visited != nodes.size()) {
            throw new DomainValidationException("Workflow contains cycles - must be a valid DAG");
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowNode> getTopologicalOrder(UUID workflowId) {
        var nodes = nodeRepository.findByWorkflowId(workflowId);
        var edges = edgeRepository.findByWorkflowId(workflowId);

        Map<UUID, WorkflowNode> nodeMap = new HashMap<>();
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        Map<UUID, Integer> inDegree = new HashMap<>();

        for (var node : nodes) {
            nodeMap.put(node.getId(), node);
            adjacency.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        for (var edge : edges) {
            adjacency.get(edge.getSourceNode().getId()).add(edge.getTargetNode().getId());
            inDegree.merge(edge.getTargetNode().getId(), 1, Integer::sum);
        }

        var queue = new LinkedList<UUID>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        var result = new ArrayList<WorkflowNode>();
        while (!queue.isEmpty()) {
            var current = queue.poll();
            result.add(nodeMap.get(current));
            for (var neighbor : adjacency.getOrDefault(current, List.of())) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Set<UUID> getRootNodeIds(UUID workflowId) {
        var nodes = nodeRepository.findByWorkflowId(workflowId);
        var edges = edgeRepository.findByWorkflowId(workflowId);
        Set<UUID> hasIncoming = new HashSet<>();
        for (var edge : edges) {
            hasIncoming.add(edge.getTargetNode().getId());
        }
        Set<UUID> roots = new HashSet<>();
        for (var node : nodes) {
            if (!hasIncoming.contains(node.getId())) {
                roots.add(node.getId());
            }
        }
        return roots;
    }
}
