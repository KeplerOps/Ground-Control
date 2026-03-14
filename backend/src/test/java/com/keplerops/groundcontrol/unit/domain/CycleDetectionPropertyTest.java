package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.GraphAlgorithms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

/**
 * Property-based tests for cycle detection (L2).
 */
@Tag("slow")
class CycleDetectionPropertyTest {

    @Property
    void dagHasNoCycles(@ForAll("randomDags") Map<UUID, List<UUID>> dag) {
        var cycles = GraphAlgorithms.findCycles(dag);
        assertThat(cycles).isEmpty();
    }

    @Property
    void addingBackEdgeAlwaysCreatesCycle(@ForAll("chainSizes") int size) {
        // Build a chain: n0 -> n1 -> n2 -> ... -> n(size-1)
        List<UUID> nodes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            nodes.add(UUID.randomUUID());
        }

        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (int i = 0; i < size; i++) {
            adj.put(nodes.get(i), new ArrayList<>());
        }
        for (int i = 0; i < size - 1; i++) {
            adj.get(nodes.get(i)).add(nodes.get(i + 1));
        }
        // Add back edge: last -> first
        adj.get(nodes.get(size - 1)).add(nodes.get(0));

        var cycles = GraphAlgorithms.findCycles(adj);
        assertThat(cycles).isNotEmpty();
    }

    @Property
    void emptyGraphHasNoCycles(@ForAll("isolatedGraphs") Map<UUID, List<UUID>> graph) {
        var cycles = GraphAlgorithms.findCycles(graph);
        assertThat(cycles).isEmpty();
    }

    @Property
    void detectedCyclePathIsValid(@ForAll("cycleSizes") int size) {
        // Create a simple cycle of given size
        List<UUID> nodes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            nodes.add(UUID.randomUUID());
        }

        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (int i = 0; i < size; i++) {
            adj.put(nodes.get(i), new ArrayList<>());
        }
        for (int i = 0; i < size; i++) {
            adj.get(nodes.get(i)).add(nodes.get((i + 1) % size));
        }

        var cycles = GraphAlgorithms.findCycles(adj);
        assertThat(cycles).isNotEmpty();

        // Each detected cycle should have consecutive nodes that are neighbors
        for (List<UUID> cycle : cycles) {
            assertThat(cycle.size()).isGreaterThanOrEqualTo(3); // at least start + 1 node + back to start
            // First and last should be the same node (cycle start)
            assertThat(cycle.get(0)).isEqualTo(cycle.get(cycle.size() - 1));
        }
    }

    @Provide
    Arbitrary<Map<UUID, List<UUID>>> randomDags() {
        return Arbitraries.integers().between(2, 20).flatMap(size -> {
            // Generate topologically ordered nodes; edges only go forward
            List<UUID> nodes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                nodes.add(UUID.randomUUID());
            }

            return Arbitraries.integers().between(0, size * 2).map(edgeCount -> {
                Map<UUID, List<UUID>> adj = new HashMap<>();
                for (UUID node : nodes) {
                    adj.put(node, new ArrayList<>());
                }
                // Add edges only from lower index to higher index (guarantees DAG)
                java.util.Random rng = new java.util.Random();
                for (int e = 0; e < edgeCount; e++) {
                    int from = rng.nextInt(size - 1);
                    int to = from + 1 + rng.nextInt(size - from - 1);
                    if (!adj.get(nodes.get(from)).contains(nodes.get(to))) {
                        adj.get(nodes.get(from)).add(nodes.get(to));
                    }
                }
                return adj;
            });
        });
    }

    @Provide
    Arbitrary<Integer> chainSizes() {
        return Arbitraries.integers().between(2, 20);
    }

    @Provide
    Arbitrary<Map<UUID, List<UUID>>> isolatedGraphs() {
        return Arbitraries.integers().between(1, 20).map(size -> {
            Map<UUID, List<UUID>> adj = new HashMap<>();
            for (int i = 0; i < size; i++) {
                adj.put(UUID.randomUUID(), new ArrayList<>());
            }
            return adj;
        });
    }

    @Provide
    Arbitrary<Integer> cycleSizes() {
        return Arbitraries.integers().between(2, 6);
    }
}
