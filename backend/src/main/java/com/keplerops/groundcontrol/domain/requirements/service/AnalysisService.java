package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
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
public class AnalysisService {

    private static final List<RelationType> DAG_TYPES =
            List.of(RelationType.PARENT, RelationType.DEPENDS_ON, RelationType.REFINES);

    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;

    public AnalysisService(
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository,
            TraceabilityLinkRepository traceabilityLinkRepository) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
    }

    /**
     * Detect cycles in the requirements DAG (PARENT, DEPENDS_ON, REFINES edges).
     *
     * @return list of cycles with member UIDs and the edges that form each cycle
     */
    public List<CycleResult> detectCycles() {
        List<RequirementRelation> relations = relationRepository.findAllWithSourceAndTargetByRelationTypeIn(DAG_TYPES);

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

    /**
     * Find orphan requirements — those with no relations and no traceability links.
     */
    public List<Requirement> findOrphans() {
        List<Requirement> allRequirements = requirementRepository.findAll();
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

    /**
     * Find requirements that lack a traceability link of the given type.
     */
    public List<Requirement> findCoverageGaps(LinkType linkType) {
        List<Requirement> allRequirements = requirementRepository.findAll();
        List<Requirement> gaps = new ArrayList<>();

        for (Requirement req : allRequirements) {
            if (!traceabilityLinkRepository.existsByRequirementIdAndLinkType(req.getId(), linkType)) {
                gaps.add(req);
            }
        }

        return gaps;
    }

    /**
     * Find all requirements transitively affected by changes to the given requirement.
     * Follows reverse DAG edges (target → sources) to find dependents, children, refinements.
     * The result includes the seed requirement.
     */
    public Set<Requirement> impactAnalysis(UUID requirementId) {
        Requirement seed = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));

        List<RequirementRelation> relations = relationRepository.findAllWithSourceAndTargetByRelationTypeIn(DAG_TYPES);

        // Build reverse adjacency list: target → list of sources (downstream = those that depend on target)
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

    /**
     * Find relations where source.wave > target.wave (backward cross-wave dependencies).
     * Relations where either wave is null are excluded.
     */
    public List<RequirementRelation> crossWaveValidation() {
        List<RequirementRelation> allRelations = relationRepository.findAllWithSourceAndTarget();
        List<RequirementRelation> violations = new ArrayList<>();

        for (RequirementRelation rel : allRelations) {
            Integer sourceWave = rel.getSource().getWave();
            Integer targetWave = rel.getTarget().getWave();
            if (sourceWave != null && targetWave != null && sourceWave > targetWave) {
                violations.add(rel);
            }
        }

        return violations;
    }
}
