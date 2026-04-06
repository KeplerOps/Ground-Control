package com.keplerops.groundcontrol.domain.verification.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.repository.VerificationResultRepository;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VerificationResultService {

    private static final Logger log = LoggerFactory.getLogger(VerificationResultService.class);

    private final VerificationResultRepository verificationResultRepository;
    private final ProjectService projectService;
    private final RequirementRepository requirementRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;

    public VerificationResultService(
            VerificationResultRepository verificationResultRepository,
            ProjectService projectService,
            RequirementRepository requirementRepository,
            TraceabilityLinkRepository traceabilityLinkRepository) {
        this.verificationResultRepository = verificationResultRepository;
        this.projectService = projectService;
        this.requirementRepository = requirementRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
    }

    public VerificationResult create(CreateVerificationResultCommand command) {
        var project = projectService.getById(command.projectId());

        var vr = new VerificationResult(
                project, command.prover(), command.result(), command.assuranceLevel(), command.verifiedAt());

        if (command.targetId() != null) {
            var target = resolveTarget(command.targetId(), project.getId());
            vr.setTarget(target);
        }
        if (command.requirementId() != null) {
            var requirement = resolveRequirement(command.requirementId(), project.getId());
            vr.setRequirement(requirement);
        }

        vr.setProperty(command.property());
        vr.setEvidence(command.evidence());
        vr.setExpiresAt(command.expiresAt());

        vr = verificationResultRepository.save(vr);
        log.info(
                "verification_result_created: id={} prover={} project={}",
                vr.getId(),
                vr.getProver(),
                project.getIdentifier());
        return vr;
    }

    public VerificationResult update(UUID projectId, UUID id, UpdateVerificationResultCommand command) {
        var vr = findOrThrow(projectId, id);

        if (command.targetId() != null) {
            var target = resolveTarget(command.targetId(), projectId);
            vr.setTarget(target);
        }
        if (command.requirementId() != null) {
            var requirement = resolveRequirement(command.requirementId(), projectId);
            vr.setRequirement(requirement);
        }
        if (command.prover() != null) {
            vr.setProver(command.prover());
        }
        if (command.property() != null) {
            vr.setProperty(command.property());
        }
        if (command.result() != null) {
            vr.setResult(command.result());
        }
        if (command.assuranceLevel() != null) {
            vr.setAssuranceLevel(command.assuranceLevel());
        }
        if (command.evidence() != null) {
            vr.setEvidence(command.evidence());
        }
        if (command.verifiedAt() != null) {
            vr.setVerifiedAt(command.verifiedAt());
        }
        if (command.expiresAt() != null) {
            vr.setExpiresAt(command.expiresAt());
        }

        vr = verificationResultRepository.save(vr);
        log.info("verification_result_updated: id={}", vr.getId());
        return vr;
    }

    @Transactional(readOnly = true)
    public VerificationResult getById(UUID projectId, UUID id) {
        return findOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public List<VerificationResult> listByProject(
            UUID projectId, UUID requirementId, String prover, VerificationStatus result) {
        if (requirementId != null) {
            return verificationResultRepository.findByProjectIdAndRequirementIdOrderByVerifiedAtDesc(
                    projectId, requirementId);
        }
        if (prover != null) {
            return verificationResultRepository.findByProjectIdAndProverOrderByVerifiedAtDesc(projectId, prover);
        }
        if (result != null) {
            return verificationResultRepository.findByProjectIdAndResultOrderByVerifiedAtDesc(projectId, result);
        }
        return verificationResultRepository.findByProjectIdOrderByVerifiedAtDesc(projectId);
    }

    public void delete(UUID projectId, UUID id) {
        var vr = findOrThrow(projectId, id);
        verificationResultRepository.delete(vr);
        log.info("verification_result_deleted: id={}", id);
    }

    private VerificationResult findOrThrow(UUID projectId, UUID id) {
        return verificationResultRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Verification result not found: " + id));
    }

    private TraceabilityLink resolveTarget(UUID targetId, UUID projectId) {
        var link = traceabilityLinkRepository
                .findById(targetId)
                .orElseThrow(() -> new NotFoundException("Traceability link not found: " + targetId));
        if (!link.getRequirement().getProject().getId().equals(projectId)) {
            throw new DomainValidationException(
                    "Traceability link does not belong to the specified project",
                    "project_mismatch",
                    Map.of("targetId", targetId.toString(), "projectId", projectId.toString()));
        }
        return link;
    }

    private Requirement resolveRequirement(UUID requirementId, UUID projectId) {
        var requirement = requirementRepository
                .findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));
        return requirement;
    }
}
