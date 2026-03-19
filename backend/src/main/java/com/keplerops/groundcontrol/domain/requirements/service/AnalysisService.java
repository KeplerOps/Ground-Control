package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
