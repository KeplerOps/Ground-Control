package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.audit.GroundControlRevisionEntity;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
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
    private final EntityManager entityManager;

    public AuditService(RequirementRepository requirementRepository, EntityManager entityManager) {
        this.requirementRepository = requirementRepository;
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
}
