package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.GraphAlgorithms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

/**
 * Property-based tests for impact analysis / reachability (L2).
 */
@Tag("slow")
class ImpactAnalysisPropertyTest {

    @Property
    void startNodeAlwaysInResult(@ForAll("randomGraphs") Map<UUID, List<UUID>> graph) {
        Assume.that(!graph.isEmpty());

        UUID start = graph.keySet().iterator().next();
        Set<UUID> result = GraphAlgorithms.findReachable(start, graph);

        assertThat(result).contains(start);
    }

    @Property
    void isolatedNodeOnlyContainsItself() {
        UUID isolated = UUID.randomUUID();
        Map<UUID, List<UUID>> graph = new HashMap<>();
        graph.put(isolated, List.of());

        Set<UUID> result = GraphAlgorithms.findReachable(isolated, graph);

        assertThat(result).containsExactly(isolated);
    }

    @Property
    void reachabilityIsTransitive(@ForAll("chainSizes") int size) {
        // Build chain: n0 -> n1 -> n2 -> ... -> n(size-1)
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

        // If B is reachable from A and C is reachable from B, then C is reachable from A
        Set<UUID> fromFirst = GraphAlgorithms.findReachable(nodes.get(0), adj);
        assertThat(fromFirst).contains(nodes.get(size - 1));

        // Every intermediate node should also be reachable
        for (UUID node : nodes) {
            assertThat(fromFirst).contains(node);
        }
    }

    @Provide
    Arbitrary<Map<UUID, List<UUID>>> randomGraphs() {
        return Arbitraries.integers().between(1, 15).flatMap(size -> {
            List<UUID> nodes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                nodes.add(UUID.randomUUID());
            }

            return Arbitraries.integers().between(0, size * 2).map(edgeCount -> {
                Map<UUID, List<UUID>> adj = new HashMap<>();
                for (UUID node : nodes) {
                    adj.put(node, new ArrayList<>());
                }
                java.util.Random rng = new java.util.Random();
                for (int e = 0; e < edgeCount; e++) {
                    int from = rng.nextInt(size);
                    int to = rng.nextInt(size);
                    if (from != to && !adj.get(nodes.get(from)).contains(nodes.get(to))) {
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
}
