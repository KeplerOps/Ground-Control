package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.audit.GroundControlRevisionEntity;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final EntityManager entityManager;

    public AuditService(
            RequirementRepository requirementRepository,
            RequirementRelationRepository relationRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            EntityManager entityManager) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.entityManager = entityManager;
    }

    public List<RequirementRevision> getRequirementHistory(UUID id) {
        if (!requirementRepository.existsById(id)) {
            throw new NotFoundException("Requirement not found: " + id);
        }

        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(Requirement.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return results.stream()
                .map(row -> {
                    var entity = (Requirement) row[0];
                    var revInfo = (GroundControlRevisionEntity) row[1];
                    var revType = (RevisionType) row[2];
                    return new RequirementRevision(
                            revInfo.getId(),
                            java.time.Instant.ofEpochMilli(revInfo.getTimestamp()),
                            revType.name(),
                            revInfo.getActor(),
                            entity);
                })
                .toList();
    }

    public List<RelationRevision> getRelationHistory(UUID relationId) {
        if (!relationRepository.existsById(relationId)) {
            throw new NotFoundException("Relation not found: " + relationId);
        }

        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(RequirementRelation.class, false, true)
                .add(AuditEntity.id().eq(relationId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return results.stream()
                .map(row -> {
                    var entity = (RequirementRelation) row[0];
                    var revInfo = (GroundControlRevisionEntity) row[1];
                    var revType = (RevisionType) row[2];
                    return new RelationRevision(
                            revInfo.getId(),
                            java.time.Instant.ofEpochMilli(revInfo.getTimestamp()),
                            revType.name(),
                            revInfo.getActor(),
                            entity);
                })
                .toList();
    }

    public List<RecentChange> getRecentRequirementChanges(Set<UUID> requirementIds, int limit) {
        if (requirementIds.isEmpty()) {
            return List.of();
        }

        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(Requirement.class, false, true)
                .add(AuditEntity.id().in(requirementIds))
                .addOrder(AuditEntity.revisionNumber().desc())
                .setMaxResults(limit)
                .getResultList();

        return results.stream()
                .map(row -> {
                    var entity = (Requirement) row[0];
                    var revInfo = (GroundControlRevisionEntity) row[1];
                    var revType = (RevisionType) row[2];
                    return new RecentChange(
                            entity.getUid(),
                            entity.getTitle(),
                            revType.name(),
                            java.time.Instant.ofEpochMilli(revInfo.getTimestamp()),
                            revInfo.getActor());
                })
                .toList();
    }

    public List<TraceabilityLinkRevision> getTraceabilityLinkHistory(UUID linkId) {
        if (!traceabilityLinkRepository.existsById(linkId)) {
            throw new NotFoundException("Traceability link not found: " + linkId);
        }

        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(TraceabilityLink.class, false, true)
                .add(AuditEntity.id().eq(linkId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        return results.stream()
                .map(row -> {
                    var entity = (TraceabilityLink) row[0];
                    var revInfo = (GroundControlRevisionEntity) row[1];
                    var revType = (RevisionType) row[2];
                    return new TraceabilityLinkRevision(
                            revInfo.getId(),
                            java.time.Instant.ofEpochMilli(revInfo.getTimestamp()),
                            revType.name(),
                            revInfo.getActor(),
                            entity);
                })
                .toList();
    }

    /**
     * Returns a unified, chronologically-sorted audit timeline for a requirement,
     * including requirement field changes, relation changes, and traceability link changes.
     */
    public List<TimelineEntry> getRequirementTimeline(UUID id, String changeCategory, Instant from, Instant to) {
        if (!requirementRepository.existsById(id)) {
            throw new NotFoundException("Requirement not found: " + id);
        }

        var entries = new ArrayList<TimelineEntry>();

        if (changeCategory == null || "REQUIREMENT".equals(changeCategory)) {
            entries.addAll(buildRequirementEntries(id));
        }
        if (changeCategory == null || "RELATION".equals(changeCategory)) {
            entries.addAll(buildRelationEntries(id));
        }
        if (changeCategory == null || "TRACEABILITY_LINK".equals(changeCategory)) {
            entries.addAll(buildTraceabilityLinkEntries(id));
        }

        // Apply date filters
        var stream = entries.stream();
        if (from != null) {
            stream = stream.filter(e -> !e.timestamp().isBefore(from));
        }
        if (to != null) {
            stream = stream.filter(e -> !e.timestamp().isAfter(to));
        }

        return stream.sorted(Comparator.comparing(TimelineEntry::timestamp).reversed())
                .toList();
    }

    private List<TimelineEntry> buildRequirementEntries(UUID requirementId) {
        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(Requirement.class, false, true)
                .add(AuditEntity.id().eq(requirementId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        var entries = new ArrayList<TimelineEntry>();
        Map<String, Object> previousSnapshot = null;

        for (Object[] row : results) {
            var entity = (Requirement) row[0];
            var revInfo = (GroundControlRevisionEntity) row[1];
            var revType = (RevisionType) row[2];

            var snapshot = requirementToSnapshot(entity);
            Map<String, FieldChange> changes = null;
            if (revType == RevisionType.MOD && previousSnapshot != null) {
                changes = computeDiff(previousSnapshot, snapshot);
            }

            entries.add(new TimelineEntry(
                    revInfo.getId(),
                    revType.name(),
                    Instant.ofEpochMilli(revInfo.getTimestamp()),
                    revInfo.getActor(),
                    "REQUIREMENT",
                    entity.getId(),
                    snapshot,
                    changes));

            if (revType != RevisionType.DEL) {
                previousSnapshot = snapshot;
            }
        }
        return entries;
    }

    private List<TimelineEntry> buildRelationEntries(UUID requirementId) {
        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(RequirementRelation.class, false, true)
                .add(AuditEntity.disjunction()
                        .add(AuditEntity.relatedId("source").eq(requirementId))
                        .add(AuditEntity.relatedId("target").eq(requirementId)))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        // Group by entity ID to compute diffs between consecutive revisions
        var previousSnapshots = new HashMap<UUID, Map<String, Object>>();
        var entries = new ArrayList<TimelineEntry>();

        for (Object[] row : results) {
            var entity = (RequirementRelation) row[0];
            var revInfo = (GroundControlRevisionEntity) row[1];
            var revType = (RevisionType) row[2];

            var snapshot = relationToSnapshot(entity);
            Map<String, FieldChange> changes = null;
            if (revType == RevisionType.MOD) {
                var prev = previousSnapshots.get(entity.getId());
                if (prev != null) {
                    changes = computeDiff(prev, snapshot);
                }
            }

            entries.add(new TimelineEntry(
                    revInfo.getId(),
                    revType.name(),
                    Instant.ofEpochMilli(revInfo.getTimestamp()),
                    revInfo.getActor(),
                    "RELATION",
                    entity.getId(),
                    snapshot,
                    changes));

            if (revType != RevisionType.DEL) {
                previousSnapshots.put(entity.getId(), snapshot);
            }
        }
        return entries;
    }

    private List<TimelineEntry> buildTraceabilityLinkEntries(UUID requirementId) {
        var auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> results = auditReader
                .createQuery()
                .forRevisionsOfEntity(TraceabilityLink.class, false, true)
                .add(AuditEntity.relatedId("requirement").eq(requirementId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        var previousSnapshots = new HashMap<UUID, Map<String, Object>>();
        var entries = new ArrayList<TimelineEntry>();

        for (Object[] row : results) {
            var entity = (TraceabilityLink) row[0];
            var revInfo = (GroundControlRevisionEntity) row[1];
            var revType = (RevisionType) row[2];

            var snapshot = traceabilityLinkToSnapshot(entity);
            Map<String, FieldChange> changes = null;
            if (revType == RevisionType.MOD) {
                var prev = previousSnapshots.get(entity.getId());
                if (prev != null) {
                    changes = computeDiff(prev, snapshot);
                }
            }

            entries.add(new TimelineEntry(
                    revInfo.getId(),
                    revType.name(),
                    Instant.ofEpochMilli(revInfo.getTimestamp()),
                    revInfo.getActor(),
                    "TRACEABILITY_LINK",
                    entity.getId(),
                    snapshot,
                    changes));

            if (revType != RevisionType.DEL) {
                previousSnapshots.put(entity.getId(), snapshot);
            }
        }
        return entries;
    }

    public static Map<String, Object> requirementToSnapshot(Requirement r) {
        var map = new LinkedHashMap<String, Object>();
        map.put("uid", r.getUid());
        map.put("title", r.getTitle());
        map.put("statement", r.getStatement());
        map.put("rationale", r.getRationale());
        map.put(
                "requirementType",
                r.getRequirementType() != null ? r.getRequirementType().name() : null);
        map.put("priority", r.getPriority() != null ? r.getPriority().name() : null);
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("wave", r.getWave());
        return map;
    }

    public static Map<String, Object> relationToSnapshot(RequirementRelation r) {
        var map = new LinkedHashMap<String, Object>();
        map.put("sourceId", r.getSource() != null ? r.getSource().getId().toString() : null);
        map.put("targetId", r.getTarget() != null ? r.getTarget().getId().toString() : null);
        map.put(
                "relationType",
                r.getRelationType() != null ? r.getRelationType().name() : null);
        map.put("description", r.getDescription());
        return map;
    }

    public static Map<String, Object> traceabilityLinkToSnapshot(TraceabilityLink t) {
        var map = new LinkedHashMap<String, Object>();
        map.put(
                "artifactType",
                t.getArtifactType() != null ? t.getArtifactType().name() : null);
        map.put("artifactIdentifier", t.getArtifactIdentifier());
        map.put("artifactUrl", t.getArtifactUrl());
        map.put("artifactTitle", t.getArtifactTitle());
        map.put("linkType", t.getLinkType() != null ? t.getLinkType().name() : null);
        map.put("syncStatus", t.getSyncStatus() != null ? t.getSyncStatus().name() : null);
        return map;
    }

    public static Map<String, FieldChange> computeDiff(Map<String, Object> previous, Map<String, Object> current) {
        var diff = new LinkedHashMap<String, FieldChange>();
        for (var entry : current.entrySet()) {
            var oldVal = previous.get(entry.getKey());
            if (!Objects.equals(oldVal, entry.getValue())) {
                diff.put(entry.getKey(), new FieldChange(oldVal, entry.getValue()));
            }
        }
        return diff.isEmpty() ? null : diff;
    }
}
