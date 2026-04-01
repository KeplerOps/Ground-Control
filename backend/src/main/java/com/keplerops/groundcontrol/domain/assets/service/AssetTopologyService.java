package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.service.GraphAlgorithms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetTopologyService {

    private final OperationalAssetRepository assetRepository;
    private final AssetRelationRepository relationRepository;

    public AssetTopologyService(
            OperationalAssetRepository assetRepository, AssetRelationRepository relationRepository) {
        this.assetRepository = assetRepository;
        this.relationRepository = relationRepository;
    }

    public List<AssetCycleResult> detectCycles(UUID projectId) {
        List<AssetRelation> relations = relationRepository.findActiveByProjectId(projectId);

        Map<UUID, List<UUID>> adjacencyList = new HashMap<>();
        Map<UUID, String> idToUid = new HashMap<>();
        Map<UUID, Map<UUID, AssetRelation>> edgeMap = new HashMap<>();

        for (AssetRelation rel : relations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            idToUid.put(sourceId, rel.getSource().getUid());
            idToUid.put(targetId, rel.getTarget().getUid());
            adjacencyList.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(targetId);
            adjacencyList.putIfAbsent(targetId, new ArrayList<>());
            edgeMap.computeIfAbsent(sourceId, k -> new HashMap<>()).put(targetId, rel);
        }

        List<List<UUID>> cycles = GraphAlgorithms.findCycles(adjacencyList);

        return cycles.stream()
                .map(cycle -> {
                    List<String> members = cycle.stream().map(idToUid::get).toList();
                    List<AssetCycleEdge> edges = new ArrayList<>();
                    for (int i = 0; i < cycle.size() - 1; i++) {
                        UUID src = cycle.get(i);
                        UUID tgt = cycle.get(i + 1);
                        AssetRelation edge = edgeMap.getOrDefault(src, Map.of()).get(tgt);
                        edges.add(new AssetCycleEdge(
                                idToUid.get(src), idToUid.get(tgt), edge != null ? edge.getRelationType() : null));
                    }
                    return new AssetCycleResult(members, edges);
                })
                .toList();
    }

    public Set<OperationalAsset> impactAnalysis(UUID assetId) {
        OperationalAsset seed = assetRepository
                .findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetId));

        UUID projectId = seed.getProject().getId();
        List<AssetRelation> relations = relationRepository.findActiveByProjectId(projectId);

        Map<UUID, List<UUID>> reverseAdj = new HashMap<>();
        for (AssetRelation rel : relations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            reverseAdj.computeIfAbsent(targetId, k -> new ArrayList<>()).add(sourceId);
        }

        Set<UUID> reachableIds = GraphAlgorithms.findReachable(assetId, reverseAdj);

        return assetRepository.findAllById(reachableIds).stream().collect(Collectors.toSet());
    }

    public AssetSubgraphResult extractSubgraph(UUID projectId, List<String> rootUids) {
        List<OperationalAsset> roots = rootUids.stream()
                .map(uid -> assetRepository
                        .findByProjectIdAndUidIgnoreCase(projectId, uid)
                        .orElseThrow(() -> new NotFoundException("Asset not found: " + uid)))
                .toList();

        Set<UUID> rootIds = roots.stream().map(OperationalAsset::getId).collect(Collectors.toSet());

        List<AssetRelation> allRelations = relationRepository.findActiveByProjectId(projectId);

        Map<UUID, List<UUID>> bidirectional = new HashMap<>();
        for (AssetRelation rel : allRelations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            bidirectional.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(targetId);
            bidirectional.computeIfAbsent(targetId, k -> new ArrayList<>()).add(sourceId);
        }

        Set<UUID> reachable = GraphAlgorithms.findReachableFromMultiple(rootIds, bidirectional);

        List<OperationalAsset> subgraphAssets = assetRepository.findAllById(reachable);
        List<AssetRelation> subgraphRelations = allRelations.stream()
                .filter(r -> reachable.contains(r.getSource().getId())
                        && reachable.contains(r.getTarget().getId()))
                .toList();

        return new AssetSubgraphResult(subgraphAssets, subgraphRelations);
    }
}
