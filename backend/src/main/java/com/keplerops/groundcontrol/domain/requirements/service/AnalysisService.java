package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private static final List<RelationType> DAG_TYPES =
            List.of(RelationType.PARENT, RelationType.DEPENDS_ON, RelationType.REFINES);

    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final AuditService auditService;

    public AnalysisService(
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            AuditService auditService) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.auditService = auditService;
    }

    public List<CycleResult> detectCycles(UUID projectId) {
        List<RequirementRelation> relations =
                relationRepository.findActiveByProjectAndRelationTypeIn(projectId, DAG_TYPES);

        Map<UUID, List<UUID>> adjacencyList = new HashMap<>();
        Map<UUID, String> idToUid = new HashMap<>();
        Map<UUID, Map<UUID, RelationType>> edgeTypes = new HashMap<>();

        for (RequirementRelation rel : relations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            idToUid.put(sourceId, rel.getSource().getUid());
            idToUid.put(targetId, rel.getTarget().getUid());
            adjacencyList.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(targetId);
            adjacencyList.putIfAbsent(targetId, new ArrayList<>());
            edgeTypes.computeIfAbsent(sourceId, k -> new HashMap<>()).put(targetId, rel.getRelationType());
        }

        List<List<UUID>> cycles = GraphAlgorithms.findCycles(adjacencyList);

        return cycles.stream()
                .map(cycle -> {
                    List<String> members = cycle.stream().map(idToUid::get).collect(Collectors.toList());
                    List<CycleEdge> edges = new ArrayList<>();
                    for (int i = 0; i < cycle.size() - 1; i++) {
                        UUID src = cycle.get(i);
                        UUID tgt = cycle.get(i + 1);
                        RelationType type =
                                edgeTypes.getOrDefault(src, Map.of()).get(tgt);
                        edges.add(new CycleEdge(idToUid.get(src), idToUid.get(tgt), type));
                    }
                    return new CycleResult(members, edges);
                })
                .collect(Collectors.toList());
    }

    public List<Requirement> findOrphans(UUID projectId) {
        List<Requirement> allRequirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);
        List<Requirement> orphans = new ArrayList<>();

        for (Requirement req : allRequirements) {
            UUID id = req.getId();
            boolean hasRelations = !relationRepository.findBySourceId(id).isEmpty()
                    || !relationRepository.findByTargetId(id).isEmpty();
            boolean hasLinks = traceabilityLinkRepository.existsByRequirementId(id);

            if (!hasRelations && !hasLinks) {
                orphans.add(req);
            }
        }

        return orphans;
    }

    public List<Requirement> findCoverageGaps(UUID projectId, LinkType linkType) {
        List<Requirement> allRequirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);
        List<Requirement> gaps = new ArrayList<>();

        for (Requirement req : allRequirements) {
            if (!traceabilityLinkRepository.existsByRequirementIdAndLinkType(req.getId(), linkType)) {
                gaps.add(req);
            }
        }

        return gaps;
    }

    public Set<Requirement> impactAnalysis(UUID requirementId) {
        Requirement seed = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));

        UUID projectId = seed.getProject().getId();
        List<RequirementRelation> relations =
                relationRepository.findActiveByProjectAndRelationTypeIn(projectId, DAG_TYPES);

        // Build reverse adjacency list: target -> list of sources (downstream = those that depend on target)
        Map<UUID, List<UUID>> reverseAdj = new HashMap<>();
        for (RequirementRelation rel : relations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            reverseAdj.computeIfAbsent(targetId, k -> new ArrayList<>()).add(sourceId);
        }

        Set<UUID> reachableIds = GraphAlgorithms.findReachable(requirementId, reverseAdj);

        return reachableIds.stream()
                .map(id -> id.equals(seed.getId())
                        ? seed
                        : requirementRepository
                                .findById(id)
                                .orElseThrow(() -> new NotFoundException("Requirement not found: " + id)))
                .collect(Collectors.toSet());
    }

    public List<ConsistencyViolation> detectConsistencyViolations(UUID projectId) {
        List<RequirementRelation> allRelations = relationRepository.findActiveWithSourceAndTargetByProjectId(projectId);
        List<ConsistencyViolation> violations = new ArrayList<>();

        for (RequirementRelation rel : allRelations) {
            Status sourceStatus = rel.getSource().getStatus();
            Status targetStatus = rel.getTarget().getStatus();
            boolean bothActive = sourceStatus == Status.ACTIVE && targetStatus == Status.ACTIVE;

            if (bothActive && rel.getRelationType() == RelationType.CONFLICTS_WITH) {
                violations.add(new ConsistencyViolation(rel, "ACTIVE_CONFLICT"));
            } else if (bothActive && rel.getRelationType() == RelationType.SUPERSEDES) {
                violations.add(new ConsistencyViolation(rel, "ACTIVE_SUPERSEDES"));
            }
        }

        return violations;
    }

    public CompletenessResult analyzeCompleteness(UUID projectId) {
        List<Requirement> allRequirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        List<CompletenessIssue> issues = new ArrayList<>();

        for (Requirement req : allRequirements) {
            String status = req.getStatus().name();
            byStatus.merge(status, 1, Integer::sum);

            if (req.getTitle() == null || req.getTitle().isBlank()) {
                issues.add(new CompletenessIssue(req.getUid(), "missing title"));
            }
            if (req.getStatement() == null || req.getStatement().isBlank()) {
                issues.add(new CompletenessIssue(req.getUid(), "missing statement"));
            }
        }

        return new CompletenessResult(allRequirements.size(), byStatus, issues);
    }

    public List<RequirementRelation> crossWaveValidation(UUID projectId) {
        List<RequirementRelation> allRelations = relationRepository.findActiveWithSourceAndTargetByProjectId(projectId);
        List<RequirementRelation> violations = new ArrayList<>();

        for (RequirementRelation rel : allRelations) {
            Integer sourceWave = rel.getSource().getWave();
            Integer targetWave = rel.getTarget().getWave();
            if (sourceWave != null && targetWave != null && sourceWave < targetWave) {
                violations.add(rel);
            }
        }

        return violations;
    }

    private static final Set<Status> SATISFIED_STATUSES = Set.of(Status.ACTIVE, Status.DEPRECATED, Status.ARCHIVED);

    private static final Map<Priority, Integer> PRIORITY_ORDER =
            Map.of(Priority.MUST, 0, Priority.SHOULD, 1, Priority.COULD, 2, Priority.WONT, 3);

    public WorkOrderResult getWorkOrder(UUID projectId) {
        List<Requirement> allRequirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);
        List<RequirementRelation> relations =
                relationRepository.findActiveByProjectAndRelationTypeIn(projectId, DAG_TYPES);

        // Index requirements by ID
        Map<UUID, Requirement> reqById = new HashMap<>();
        for (Requirement req : allRequirements) {
            reqById.put(req.getId(), req);
        }

        // Build dependsOn map: source -> [targets it depends on]
        Map<UUID, List<UUID>> dependsOn = new HashMap<>();
        for (RequirementRelation rel : relations) {
            UUID sourceId = rel.getSource().getId();
            UUID targetId = rel.getTarget().getId();
            // Only include edges where both endpoints are in our non-archived set
            if (reqById.containsKey(sourceId) && reqById.containsKey(targetId)) {
                dependsOn.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(targetId);
            }
        }

        // Determine blocking status for each requirement
        Map<UUID, BlockingStatus> blockingStatusMap = new HashMap<>();
        Map<UUID, List<String>> blockedByMap = new HashMap<>();

        for (Requirement req : allRequirements) {
            UUID id = req.getId();
            List<UUID> deps = dependsOn.getOrDefault(id, List.of());
            if (deps.isEmpty()) {
                blockingStatusMap.put(id, BlockingStatus.UNCONSTRAINED);
                blockedByMap.put(id, List.of());
            } else {
                List<String> blockers = new ArrayList<>();
                for (UUID depId : deps) {
                    Requirement dep = reqById.get(depId);
                    if (dep != null && !SATISFIED_STATUSES.contains(dep.getStatus())) {
                        blockers.add(dep.getUid());
                    }
                }
                if (blockers.isEmpty()) {
                    blockingStatusMap.put(id, BlockingStatus.UNBLOCKED);
                } else {
                    blockingStatusMap.put(id, BlockingStatus.BLOCKED);
                }
                blockedByMap.put(id, blockers);
            }
        }

        // Group by wave (nulls-last)
        Map<Integer, List<Requirement>> byWave = new TreeMap<>(Comparator.nullsLast(Comparator.naturalOrder()));
        for (Requirement req : allRequirements) {
            byWave.computeIfAbsent(req.getWave(), k -> new ArrayList<>()).add(req);
        }

        // Build work order waves
        int globalOrder = 0;
        int totalUnblocked = 0;
        int totalBlocked = 0;
        int totalUnconstrained = 0;
        List<WorkOrderWave> waves = new ArrayList<>();

        for (Map.Entry<Integer, List<Requirement>> entry : byWave.entrySet()) {
            Integer wave = entry.getKey();
            List<Requirement> waveReqs = entry.getValue();

            // Build intra-wave dependency subgraph
            Set<UUID> waveIds = new HashSet<>();
            for (Requirement req : waveReqs) {
                waveIds.add(req.getId());
            }
            Map<UUID, List<UUID>> waveDeps = new HashMap<>();
            for (Requirement req : waveReqs) {
                UUID id = req.getId();
                List<UUID> deps = dependsOn.getOrDefault(id, List.of());
                List<UUID> intraWaveDeps = new ArrayList<>();
                for (UUID dep : deps) {
                    if (waveIds.contains(dep)) {
                        intraWaveDeps.add(dep);
                    }
                }
                waveDeps.put(id, intraWaveDeps);
            }

            // Topo-sort within wave using MoSCoW priority as tie-breaker
            Map<UUID, Priority> priorityMap = new HashMap<>();
            for (Requirement req : waveReqs) {
                priorityMap.put(req.getId(), req.getPriority());
            }
            Comparator<UUID> tieBreaker = Comparator.comparingInt(
                    id -> PRIORITY_ORDER.getOrDefault(priorityMap.getOrDefault(id, Priority.WONT), 3));

            List<UUID> sorted = GraphAlgorithms.topologicalSort(waveDeps, tieBreaker);

            // Append any nodes not in sorted result (cycle participants), sorted by priority
            Set<UUID> sortedSet = new HashSet<>(sorted);
            List<UUID> remaining = new ArrayList<>();
            for (Requirement req : waveReqs) {
                if (!sortedSet.contains(req.getId())) {
                    remaining.add(req.getId());
                }
            }
            remaining.sort(tieBreaker);
            sorted.addAll(remaining);

            // Build items
            int waveUnblocked = 0;
            int waveBlocked = 0;
            int waveUnconstrained = 0;
            List<WorkOrderItem> items = new ArrayList<>();

            for (UUID id : sorted) {
                Requirement req = reqById.get(id);
                BlockingStatus bs = blockingStatusMap.get(id);
                switch (bs) {
                    case UNBLOCKED -> waveUnblocked++;
                    case BLOCKED -> waveBlocked++;
                    case UNCONSTRAINED -> waveUnconstrained++;
                    default -> throw new IllegalStateException("Unexpected blocking status: " + bs);
                }
                items.add(new WorkOrderItem(
                        id,
                        req.getUid(),
                        req.getTitle(),
                        req.getStatus().name(),
                        req.getPriority().name(),
                        req.getWave(),
                        globalOrder++,
                        bs,
                        blockedByMap.getOrDefault(id, List.of())));
            }

            totalUnblocked += waveUnblocked;
            totalBlocked += waveBlocked;
            totalUnconstrained += waveUnconstrained;

            waves.add(new WorkOrderWave(wave, waveReqs.size(), waveUnblocked, waveBlocked, waveUnconstrained, items));
        }

        return new WorkOrderResult(allRequirements.size(), totalUnblocked, totalBlocked, totalUnconstrained, waves);
    }

    public DashboardStats getDashboardStats(UUID projectId) {
        List<Requirement> allRequirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);

        // byStatus — single pass
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (Requirement req : allRequirements) {
            byStatus.merge(req.getStatus().name(), 1, Integer::sum);
        }

        // byWave — group by wave, count by status per group
        // TreeMap with nulls-first comparator so null waves sort first
        Map<Integer, List<Requirement>> byWaveGroup = new TreeMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        for (Requirement req : allRequirements) {
            byWaveGroup.computeIfAbsent(req.getWave(), k -> new ArrayList<>()).add(req);
        }
        List<WaveStats> byWave = byWaveGroup.entrySet().stream()
                .map(entry -> {
                    Map<String, Integer> waveByStatus = new LinkedHashMap<>();
                    for (Requirement req : entry.getValue()) {
                        waveByStatus.merge(req.getStatus().name(), 1, Integer::sum);
                    }
                    return new WaveStats(entry.getKey(), entry.getValue().size(), waveByStatus);
                })
                .toList();

        // coverageByLinkType — for each LinkType, count covered requirements
        int total = allRequirements.size();
        Map<String, CoverageStats> coverageByLinkType = new LinkedHashMap<>();
        for (LinkType linkType : LinkType.values()) {
            int covered = 0;
            for (Requirement req : allRequirements) {
                if (traceabilityLinkRepository.existsByRequirementIdAndLinkType(req.getId(), linkType)) {
                    covered++;
                }
            }
            double percentage = total > 0 ? Math.round(covered * 1000.0 / total) / 10.0 : 0.0;
            coverageByLinkType.put(linkType.name(), new CoverageStats(total, covered, percentage));
        }

        // recentChanges — delegate to AuditService
        Set<UUID> reqIds = allRequirements.stream().map(Requirement::getId).collect(Collectors.toSet());
        List<RecentChange> recentChanges = auditService.getRecentRequirementChanges(reqIds, 10);

        return new DashboardStats(total, byStatus, byWave, coverageByLinkType, recentChanges);
    }
}
