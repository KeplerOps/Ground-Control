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
import java.util.List;
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
}
