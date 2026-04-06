package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MixedGraphService {

    private final MixedGraphClient mixedGraphClient;

    public MixedGraphService(MixedGraphClient mixedGraphClient) {
        this.mixedGraphClient = mixedGraphClient;
    }

    public GraphProjection getVisualization(UUID projectId, List<String> entityTypeNames) {
        return filterProjection(mixedGraphClient.getVisualization(projectId), parseEntityTypes(entityTypeNames));
    }

    public GraphProjection extractSubgraph(
            UUID projectId, List<String> rootNodeIds, int maxDepth, List<String> entityTypeNames) {
        return neighborhoodProjection(projectId, rootNodeIds, maxDepth, entityTypeNames);
    }

    public GraphProjection traverse(
            UUID projectId, List<String> rootNodeIds, int maxDepth, List<String> entityTypeNames) {
        return neighborhoodProjection(projectId, rootNodeIds, maxDepth, entityTypeNames);
    }

    public List<GraphPathResult> findPaths(
            UUID projectId, String sourceNodeId, String targetNodeId, int maxDepth, List<String> entityTypeNames) {
        var projection =
                filterProjection(mixedGraphClient.getVisualization(projectId), parseEntityTypes(entityTypeNames));
        var nodesById = projection.nodes().stream().collect(Collectors.toMap(GraphNode::id, node -> node));
        if (!nodesById.containsKey(sourceNodeId)) {
            throw new NotFoundException("Graph node not found: " + sourceNodeId);
        }
        if (!nodesById.containsKey(targetNodeId)) {
            throw new NotFoundException("Graph node not found: " + targetNodeId);
        }
        var adjacency = buildAdjacency(projection.edges());
        var edgeLookup = buildEdgeLookup(projection.edges());

        Deque<List<String>> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(List.of(sourceNodeId));
        visited.add(sourceNodeId);

        while (!queue.isEmpty()) {
            var path = queue.removeFirst();
            var current = path.getLast();
            if (current.equals(targetNodeId)) {
                List<String> edgeTypes = new ArrayList<>();
                for (int i = 0; i < path.size() - 1; i++) {
                    var edge = edgeLookup.get(undirectedKey(path.get(i), path.get(i + 1)));
                    edgeTypes.add(edge != null ? edge.edgeType() : "RELATED");
                }
                return List.of(new GraphPathResult(path, edgeTypes));
            }
            if (path.size() > maxDepth + 1) {
                continue;
            }
            for (String neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    List<String> nextPath = new ArrayList<>(path);
                    nextPath.add(neighbor);
                    queue.addLast(nextPath);
                }
            }
        }
        return List.of();
    }

    private GraphProjection neighborhoodProjection(
            UUID projectId, List<String> rootNodeIds, int maxDepth, List<String> entityTypeNames) {
        var projection =
                filterProjection(mixedGraphClient.getVisualization(projectId), parseEntityTypes(entityTypeNames));
        var nodesById = projection.nodes().stream().collect(Collectors.toMap(GraphNode::id, node -> node));
        for (String rootNodeId : rootNodeIds) {
            if (!nodesById.containsKey(rootNodeId)) {
                throw new NotFoundException("Graph node not found: " + rootNodeId);
            }
        }

        var adjacency = buildAdjacency(projection.edges());
        var visited = new LinkedHashSet<String>();
        var queue = new ArrayDeque<Map.Entry<String, Integer>>();
        for (String rootNodeId : rootNodeIds) {
            visited.add(rootNodeId);
            queue.add(Map.entry(rootNodeId, 0));
        }

        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            if (current.getValue() >= maxDepth) {
                continue;
            }
            for (String neighbor : adjacency.getOrDefault(current.getKey(), Set.of())) {
                if (visited.add(neighbor)) {
                    queue.addLast(Map.entry(neighbor, current.getValue() + 1));
                }
            }
        }

        var nodes = projection.nodes().stream()
                .filter(node -> visited.contains(node.id()))
                .toList();
        var edges = projection.edges().stream()
                .filter(edge -> visited.contains(edge.sourceId()) && visited.contains(edge.targetId()))
                .toList();
        return new GraphProjection(nodes, edges);
    }

    private GraphProjection filterProjection(GraphProjection projection, Set<GraphEntityType> entityTypes) {
        if (entityTypes.isEmpty()) {
            return projection;
        }
        var nodes = projection.nodes().stream()
                .filter(node -> entityTypes.contains(node.entityType()))
                .toList();
        var visibleNodeIds = nodes.stream().map(GraphNode::id).collect(Collectors.toSet());
        var edges = projection.edges().stream()
                .filter(edge -> visibleNodeIds.contains(edge.sourceId()) && visibleNodeIds.contains(edge.targetId()))
                .toList();
        return new GraphProjection(nodes, edges);
    }

    private Set<GraphEntityType> parseEntityTypes(List<String> entityTypeNames) {
        if (entityTypeNames == null || entityTypeNames.isEmpty()) {
            return Set.of();
        }
        return entityTypeNames.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(GraphEntityType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Set<String>> buildAdjacency(List<GraphEdge> edges) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (GraphEdge edge : edges) {
            adjacency
                    .computeIfAbsent(edge.sourceId(), ignored -> new LinkedHashSet<>())
                    .add(edge.targetId());
            adjacency
                    .computeIfAbsent(edge.targetId(), ignored -> new LinkedHashSet<>())
                    .add(edge.sourceId());
        }
        return adjacency;
    }

    private Map<String, GraphEdge> buildEdgeLookup(List<GraphEdge> edges) {
        Map<String, GraphEdge> lookup = new LinkedHashMap<>();
        for (GraphEdge edge : edges) {
            lookup.putIfAbsent(undirectedKey(edge.sourceId(), edge.targetId()), edge);
        }
        return lookup;
    }

    private String undirectedKey(String left, String right) {
        return left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left;
    }
}
