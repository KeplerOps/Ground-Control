package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementSpecifications;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;

    public RequirementService(
            RequirementRepository requirementRepository, RequirementRelationRepository relationRepository) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
    }

    public Requirement create(CreateRequirementCommand command) {
        if (requirementRepository.existsByUid(command.uid())) {
            throw new ConflictException("Requirement with UID '" + command.uid() + "' already exists");
        }

        var requirement = new Requirement(command.uid(), command.title(), command.statement());
        if (command.rationale() != null) {
            requirement.setRationale(command.rationale());
        }
        if (command.requirementType() != null) {
            requirement.setRequirementType(command.requirementType());
        }
        if (command.priority() != null) {
            requirement.setPriority(command.priority());
        }
        requirement.setWave(command.wave());
        return requirementRepository.save(requirement);
    }

    @Transactional(readOnly = true)
    public Requirement getById(UUID id) {
        return requirementRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + id));
    }

    @Transactional(readOnly = true)
    public Requirement getByUid(String uid) {
        return requirementRepository
                .findByUid(uid)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + uid));
    }

    public Requirement update(UUID id, UpdateRequirementCommand command) {
        var requirement = getById(id);
        if (command.title() != null) {
            requirement.setTitle(command.title());
        }
        if (command.statement() != null) {
            requirement.setStatement(command.statement());
        }
        if (command.rationale() != null) {
            requirement.setRationale(command.rationale());
        }
        if (command.requirementType() != null) {
            requirement.setRequirementType(command.requirementType());
        }
        if (command.priority() != null) {
            requirement.setPriority(command.priority());
        }
        requirement.setWave(command.wave());
        return requirementRepository.save(requirement);
    }

    /*@ requires id != null;
    @ requires newStatus != null;
    @ ensures \result.getStatus() == newStatus;
    @ signals (NotFoundException e) !requirementRepository.existsById(id);
    @ signals (DomainValidationException e) !\old(getById(id)).getStatus().canTransitionTo(newStatus); @*/
    public Requirement transitionStatus(UUID id, Status newStatus) {
        var requirement = getById(id);
        requirement.transitionStatus(newStatus);
        return requirementRepository.save(requirement);
    }

    public BulkTransitionResult bulkTransitionStatus(List<UUID> ids, Status newStatus) {
        var succeeded = new ArrayList<Requirement>();
        var failed = new ArrayList<Map<String, Object>>();
        for (UUID id : ids) {
            try {
                var requirement = getById(id);
                if (!requirement.getStatus().canTransitionTo(newStatus)) {
                    failed.add(Map.of(
                            "id", id.toString(),
                            "uid", requirement.getUid(),
                            "error", "Cannot transition from " + requirement.getStatus() + " to " + newStatus));
                    continue;
                }
                requirement.transitionStatus(newStatus);
                succeeded.add(requirementRepository.save(requirement));
            } catch (NotFoundException e) {
                failed.add(Map.of("id", id.toString(), "error", e.getMessage()));
            }
        }
        return new BulkTransitionResult(succeeded, failed);
    }

    /*@ requires id != null;
    @ ensures \result.getStatus() == Status.ARCHIVED;
    @ ensures \result.getArchivedAt() != null;
    @ signals (NotFoundException e) !requirementRepository.existsById(id);
    @ signals (DomainValidationException e) !\old(getById(id)).getStatus().canTransitionTo(Status.ARCHIVED); @*/
    public Requirement archive(UUID id) {
        var requirement = getById(id);
        requirement.archive();
        return requirementRepository.save(requirement);
    }

    /*@ requires sourceId != null && targetId != null && relationType != null;
    @ requires !sourceId.equals(targetId);
    @ ensures \result != null;
    @ signals (NotFoundException e) !requirementRepository.existsById(sourceId)
    @                             || !requirementRepository.existsById(targetId);
    @ signals (DomainValidationException e) sourceId.equals(targetId); @*/
    public RequirementRelation createRelation(UUID sourceId, UUID targetId, RelationType relationType) {
        if (sourceId.equals(targetId)) {
            throw new DomainValidationException("A requirement cannot relate to itself");
        }
        if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(sourceId, targetId, relationType)) {
            throw new ConflictException(
                    "Relation " + relationType + " already exists between " + sourceId + " and " + targetId);
        }
        var source = getById(sourceId);
        var target = getById(targetId);
        var relation = new RequirementRelation(source, target, relationType);
        return relationRepository.save(relation);
    }

    @Transactional(readOnly = true)
    public List<RequirementRelation> getRelations(UUID requirementId) {
        // Verify requirement exists
        getById(requirementId);
        var outgoing = relationRepository.findBySourceIdWithEntities(requirementId);
        var incoming = relationRepository.findByTargetIdWithEntities(requirementId);
        outgoing.addAll(incoming);
        return outgoing;
    }

    @Transactional(readOnly = true)
    public Page<Requirement> list(Pageable pageable, RequirementFilter filter) {
        var spec = RequirementSpecifications.fromFilter(filter);
        return requirementRepository.findAll(spec, pageable);
    }

    public Requirement clone(UUID sourceId, CloneRequirementCommand command) {
        var source = getById(sourceId);

        if (requirementRepository.existsByUid(command.newUid())) {
            throw new ConflictException("Requirement with UID '" + command.newUid() + "' already exists");
        }

        var clone = new Requirement(command.newUid(), source.getTitle(), source.getStatement());
        clone.setRationale(source.getRationale());
        clone.setRequirementType(source.getRequirementType());
        clone.setPriority(source.getPriority());
        clone.setWave(source.getWave());
        clone = requirementRepository.save(clone);

        if (command.copyRelations()) {
            for (var rel : relationRepository.findBySourceIdWithEntities(sourceId)) {
                var clonedRelation = new RequirementRelation(clone, rel.getTarget(), rel.getRelationType());
                relationRepository.save(clonedRelation);
            }
        }

        return clone;
    }

    public void deleteRelation(UUID requirementId, UUID relationId) {
        var relation = relationRepository
                .findById(relationId)
                .orElseThrow(() -> new NotFoundException("Relation not found: " + relationId));

        var sourceId = relation.getSource().getId();
        var targetId = relation.getTarget().getId();
        if (!requirementId.equals(sourceId) && !requirementId.equals(targetId)) {
            throw new NotFoundException("Relation " + relationId + " does not belong to requirement " + requirementId);
        }
        relationRepository.delete(relation);
    }
}
