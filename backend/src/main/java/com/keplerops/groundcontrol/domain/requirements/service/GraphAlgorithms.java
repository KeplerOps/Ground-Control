package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Pure graph algorithms for cycle detection and reachability.
 * No Spring dependencies — designed for direct property testing.
 */
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public final class GraphAlgorithms {

    private static final int WHITE = 0;
    private static final int GRAY = 1;
    private static final int BLACK = 2;

    private GraphAlgorithms() {}

    /**
     * Detect all cycles in a directed graph using DFS with three-color marking.
     *
     * @param adjacencyList map from node to its outgoing neighbors
     * @return list of cycles, where each cycle is an ordered list of node UUIDs
     */
    /*@ ensures \result != null; @*/
    public static List<List<UUID>> findCycles(Map<UUID, List<UUID>> adjacencyList) {
        Map<UUID, Integer> color = new HashMap<>();
        Map<UUID, UUID> parent = new HashMap<>();
        List<List<UUID>> cycles = new ArrayList<>();

        for (UUID node : adjacencyList.keySet()) {
            color.put(node, WHITE);
        }

        for (UUID node : adjacencyList.keySet()) {
            if (color.get(node) == WHITE) {
                dfs(node, adjacencyList, color, parent, cycles);
            }
        }

        return cycles;
    }

    private static void dfs(
            UUID node,
            Map<UUID, List<UUID>> adjacencyList,
            Map<UUID, Integer> color,
            Map<UUID, UUID> parent,
            List<List<UUID>> cycles) {
        color.put(node, GRAY);

        for (UUID neighbor : adjacencyList.getOrDefault(node, List.of())) {
            Integer neighborColor = color.get(neighbor);
            if (neighborColor == null) {
                // Node not in adjacency list keys — skip
                continue;
            }
            if (neighborColor == WHITE) {
                parent.put(neighbor, node);
                dfs(neighbor, adjacencyList, color, parent, cycles);
            } else if (neighborColor == GRAY) {
                // Back edge found — reconstruct cycle
                List<UUID> cycle = reconstructCycle(node, neighbor, parent);
                cycles.add(cycle);
            }
        }

        color.put(node, BLACK);
    }

    private static List<UUID> reconstructCycle(UUID current, UUID cycleStart, Map<UUID, UUID> parent) {
        ArrayDeque<UUID> cycle = new ArrayDeque<>();
        cycle.addFirst(cycleStart);
        UUID node = current;
        while (!node.equals(cycleStart)) {
            cycle.addFirst(node);
            node = parent.get(node);
        }
        cycle.addFirst(cycleStart);
        return List.copyOf(cycle);
    }

    /**
     * Find all nodes reachable from the start node via BFS, including the start node itself.
     *
     * @param start the starting node
     * @param adjacencyList map from node to its outgoing neighbors
     * @return set of all reachable nodes (always includes start)
     */
    /*@ requires start != null;
    @ ensures \result != null;
    @ ensures \result.contains(start); @*/
    public static Set<UUID> findReachable(UUID start, Map<UUID, List<UUID>> adjacencyList) {
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            UUID node = queue.poll();
            for (UUID neighbor : adjacencyList.getOrDefault(node, List.of())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }
}
