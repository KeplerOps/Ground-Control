package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackInstallRecordRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PackInstallOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PackInstallOrchestrator.class);

    private static String sanitizeForLog(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\r\\n\\t]", "_");
    }

    private final PackResolver packResolver;
    private final TrustEvaluator trustEvaluator;
    private final PackInstallRecordRepository installRecordRepository;
    private final ControlPackService controlPackService;
    private final ProjectService projectService;

    public PackInstallOrchestrator(
            PackResolver packResolver,
            TrustEvaluator trustEvaluator,
            PackInstallRecordRepository installRecordRepository,
            ControlPackService controlPackService,
            ProjectService projectService) {
        this.packResolver = packResolver;
        this.trustEvaluator = trustEvaluator;
        this.installRecordRepository = installRecordRepository;
        this.controlPackService = controlPackService;
        this.projectService = projectService;
    }

    public PackInstallRecord installPack(InstallPackCommand command) {
        var project = projectService.getById(command.projectId());
        log.info(
                "pack_install_orchestration_started: pack_id={}, version_constraint={}",
                sanitizeForLog(command.packId()),
                sanitizeForLog(command.versionConstraint()));

        var gate = resolveAndEvaluateTrust(command, project);
        if (gate.rejectionRecord() != null) {
            return gate.rejectionRecord();
        }

        try {
            validateControlPackEntries(gate.entry(), command, "installation");
            var installCommand = new InstallControlPackCommand(
                    command.projectId(),
                    gate.entry().getPackId(),
                    gate.resolved().resolvedVersion(),
                    gate.entry().getPublisher(),
                    gate.entry().getDescription(),
                    gate.entry().getSourceUrl(),
                    gate.resolved().resolvedChecksum(),
                    gate.entry().getCompatibility(),
                    gate.entry().getRegistryMetadata(),
                    command.entries());
            var result = controlPackService.install(installCommand);
            var record = buildRecord(
                    project, command, gate.entry(), gate.resolved(), gate.trust(), InstallOutcome.INSTALLED);
            record.setInstalledEntityId(result.controlPack().getId());
            var saved = installRecordRepository.save(record);
            log.info(
                    "pack_install_completed: pack_id={}, version={}, controls_created={}",
                    gate.entry().getPackId(),
                    gate.resolved().resolvedVersion(),
                    result.controlsCreated());
            return saved;
        } catch (Exception e) {
            var record =
                    buildRecord(project, command, gate.entry(), gate.resolved(), gate.trust(), InstallOutcome.FAILED);
            record.setErrorDetail("Installation failed: " + e.getMessage());
            log.info("pack_install_failed: pack_id={}, error={}", gate.entry().getPackId(), e.getMessage());
            return installRecordRepository.save(record);
        }
    }

    public PackInstallRecord upgradePack(InstallPackCommand command) {
        var project = projectService.getById(command.projectId());
        log.info(
                "pack_upgrade_orchestration_started: pack_id={}, version_constraint={}",
                sanitizeForLog(command.packId()),
                sanitizeForLog(command.versionConstraint()));

        var gate = resolveAndEvaluateTrust(command, project);
        if (gate.rejectionRecord() != null) {
            return gate.rejectionRecord();
        }

        try {
            validateControlPackEntries(gate.entry(), command, "upgrade");
            var upgradeCommand = new UpgradeControlPackCommand(
                    command.projectId(),
                    gate.entry().getPackId(),
                    gate.resolved().resolvedVersion(),
                    gate.entry().getPublisher(),
                    gate.entry().getDescription(),
                    gate.entry().getSourceUrl(),
                    gate.resolved().resolvedChecksum(),
                    gate.entry().getCompatibility(),
                    gate.entry().getRegistryMetadata(),
                    command.entries());
            var result = controlPackService.upgrade(upgradeCommand);
            var record =
                    buildRecord(project, command, gate.entry(), gate.resolved(), gate.trust(), InstallOutcome.UPGRADED);
            record.setInstalledEntityId(result.controlPack().getId());
            var saved = installRecordRepository.save(record);
            log.info(
                    "pack_upgrade_completed: pack_id={}, version={}, previous_version={}",
                    gate.entry().getPackId(),
                    gate.resolved().resolvedVersion(),
                    result.previousVersion());
            return saved;
        } catch (Exception e) {
            var record =
                    buildRecord(project, command, gate.entry(), gate.resolved(), gate.trust(), InstallOutcome.FAILED);
            record.setErrorDetail("Upgrade failed: " + e.getMessage());
            log.info("pack_upgrade_failed: pack_id={}, error={}", gate.entry().getPackId(), e.getMessage());
            return installRecordRepository.save(record);
        }
    }

    @Transactional(readOnly = true)
    public List<PackInstallRecord> listInstallRecords(UUID projectId) {
        return installRecordRepository.findByProjectIdOrderByPerformedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<PackInstallRecord> listInstallRecords(UUID projectId, String packId) {
        return installRecordRepository.findByProjectIdAndPackIdOrderByPerformedAtDesc(projectId, packId);
    }

    @Transactional(readOnly = true)
    public PackInstallRecord getInstallRecord(UUID recordId) {
        return installRecordRepository
                .findById(recordId)
                .orElseThrow(() -> new NotFoundException("Install record not found: " + recordId));
    }

    private record GateResult(
            ResolvedPack resolved, PackRegistryEntry entry, TrustDecision trust, PackInstallRecord rejectionRecord) {}

    private GateResult resolveAndEvaluateTrust(InstallPackCommand command, Project project) {
        ResolvedPack resolvedPack;
        try {
            resolvedPack = packResolver.resolve(command.projectId(), command.packId(), command.versionConstraint());
        } catch (NotFoundException e) {
            var record = new PackInstallRecord(
                    project, command.packId(), PackType.CONTROL_PACK, TrustOutcome.UNKNOWN, InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Resolution failed: " + e.getMessage());
            log.info("pack_install_failed: pack_id={}, reason=resolution_failed", sanitizeForLog(command.packId()));
            return new GateResult(null, null, null, installRecordRepository.save(record));
        }

        var entry = resolvedPack.entry();

        if (!packResolver.checkCompatibility(resolvedPack)) {
            var trust = new TrustDecision(TrustOutcome.UNKNOWN, null, null);
            var record = buildRecord(project, command, entry, resolvedPack, trust, InstallOutcome.REJECTED);
            record.setErrorDetail("Pack is not compatible with the current platform version");
            log.info("pack_install_rejected: pack_id={}, reason=incompatible", entry.getPackId());
            return new GateResult(resolvedPack, entry, trust, installRecordRepository.save(record));
        }

        var trustDecision = trustEvaluator.evaluate(command.projectId(), resolvedPack);
        if (trustDecision.outcome() == TrustOutcome.REJECTED) {
            var record = buildRecord(project, command, entry, resolvedPack, trustDecision, InstallOutcome.REJECTED);
            log.info("pack_install_trust_rejected: pack_id={}, policy={}", entry.getPackId(), trustDecision.policyId());
            return new GateResult(resolvedPack, entry, trustDecision, installRecordRepository.save(record));
        }

        return new GateResult(resolvedPack, entry, trustDecision, null);
    }

    private PackInstallRecord buildRecord(
            Project project,
            InstallPackCommand command,
            PackRegistryEntry entry,
            ResolvedPack resolvedPack,
            TrustDecision trustDecision,
            InstallOutcome outcome) {
        var record =
                new PackInstallRecord(project, command.packId(), entry.getPackType(), trustDecision.outcome(), outcome);
        record.setRequestedVersion(command.versionConstraint());
        record.setResolvedVersion(resolvedPack.resolvedVersion());
        record.setResolvedSource(resolvedPack.resolvedSource());
        record.setResolvedChecksum(resolvedPack.resolvedChecksum());
        record.setTrustPolicyId(trustDecision.policyId());
        record.setTrustReason(trustDecision.reason());
        record.setPerformedBy(command.performedBy());
        return record;
    }

    private void validateControlPackEntries(PackRegistryEntry entry, InstallPackCommand command, String operation) {
        if (entry.getPackType() != PackType.CONTROL_PACK) {
            throw new DomainValidationException("Only CONTROL_PACK " + operation + " is currently supported");
        }
        if (command.entries() == null || command.entries().isEmpty()) {
            throw new DomainValidationException("Control pack entries must be provided for " + operation);
        }
    }
}
