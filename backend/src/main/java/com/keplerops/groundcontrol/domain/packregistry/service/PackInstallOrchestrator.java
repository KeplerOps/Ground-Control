package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackInstallRecordRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
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
                command.packId(),
                command.versionConstraint());

        // Step 1: Resolve from registry
        ResolvedPack resolvedPack;
        try {
            resolvedPack = packResolver.resolve(command.projectId(), command.packId(), command.versionConstraint());
        } catch (NotFoundException e) {
            var record = new PackInstallRecord(
                    project, command.packId(), PackType.CONTROL_PACK, TrustOutcome.UNKNOWN, InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Resolution failed: " + e.getMessage());
            var saved = installRecordRepository.save(record);
            log.info("pack_install_failed: pack_id={}, reason=resolution_failed", command.packId());
            return saved;
        }

        var entry = resolvedPack.entry();

        // Step 2: Check compatibility
        if (!packResolver.checkCompatibility(resolvedPack)) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), TrustOutcome.UNKNOWN, InstallOutcome.REJECTED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Pack is not compatible with the current platform version");
            var saved = installRecordRepository.save(record);
            log.info("pack_install_rejected: pack_id={}, reason=incompatible", command.packId());
            return saved;
        }

        // Step 3: Evaluate trust
        var trustDecision = trustEvaluator.evaluate(command.projectId(), resolvedPack);

        if (trustDecision.outcome() == TrustOutcome.REJECTED) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), TrustOutcome.REJECTED, InstallOutcome.REJECTED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setTrustPolicyId(trustDecision.policyId());
            record.setTrustReason(trustDecision.reason());
            record.setPerformedBy(command.performedBy());
            var saved = installRecordRepository.save(record);
            log.info("pack_install_trust_rejected: pack_id={}, policy={}", command.packId(), trustDecision.policyId());
            return saved;
        }

        // Step 4: Delegate to type-specific installer
        try {
            return executeInstall(command, project, entry, resolvedPack, trustDecision);
        } catch (Exception e) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), trustDecision.outcome(), InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setTrustPolicyId(trustDecision.policyId());
            record.setTrustReason(trustDecision.reason());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Installation failed: " + e.getMessage());
            var saved = installRecordRepository.save(record);
            log.info("pack_install_failed: pack_id={}, error={}", command.packId(), e.getMessage());
            return saved;
        }
    }

    public PackInstallRecord upgradePack(InstallPackCommand command) {
        var project = projectService.getById(command.projectId());
        log.info(
                "pack_upgrade_orchestration_started: pack_id={}, version_constraint={}",
                command.packId(),
                command.versionConstraint());

        // Step 1: Resolve from registry
        ResolvedPack resolvedPack;
        try {
            resolvedPack = packResolver.resolve(command.projectId(), command.packId(), command.versionConstraint());
        } catch (NotFoundException e) {
            var record = new PackInstallRecord(
                    project, command.packId(), PackType.CONTROL_PACK, TrustOutcome.UNKNOWN, InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Resolution failed: " + e.getMessage());
            return installRecordRepository.save(record);
        }

        var entry = resolvedPack.entry();

        // Step 2: Check compatibility
        if (!packResolver.checkCompatibility(resolvedPack)) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), TrustOutcome.UNKNOWN, InstallOutcome.REJECTED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Pack is not compatible with the current platform version");
            return installRecordRepository.save(record);
        }

        // Step 3: Evaluate trust
        var trustDecision = trustEvaluator.evaluate(command.projectId(), resolvedPack);

        if (trustDecision.outcome() == TrustOutcome.REJECTED) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), TrustOutcome.REJECTED, InstallOutcome.REJECTED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setTrustPolicyId(trustDecision.policyId());
            record.setTrustReason(trustDecision.reason());
            record.setPerformedBy(command.performedBy());
            return installRecordRepository.save(record);
        }

        // Step 4: Delegate to type-specific upgrader
        try {
            return executeUpgrade(command, project, entry, resolvedPack, trustDecision);
        } catch (Exception e) {
            var record = new PackInstallRecord(
                    project, command.packId(), entry.getPackType(), trustDecision.outcome(), InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setResolvedVersion(resolvedPack.resolvedVersion());
            record.setResolvedSource(resolvedPack.resolvedSource());
            record.setResolvedChecksum(resolvedPack.resolvedChecksum());
            record.setTrustPolicyId(trustDecision.policyId());
            record.setTrustReason(trustDecision.reason());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Upgrade failed: " + e.getMessage());
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

    private PackInstallRecord executeInstall(
            InstallPackCommand command,
            com.keplerops.groundcontrol.domain.projects.model.Project project,
            com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry entry,
            ResolvedPack resolvedPack,
            TrustDecision trustDecision) {

        if (entry.getPackType() != PackType.CONTROL_PACK) {
            throw new DomainValidationException("Only CONTROL_PACK installation is currently supported");
        }

        if (command.entries() == null || command.entries().isEmpty()) {
            throw new DomainValidationException("Control pack entries must be provided for installation");
        }

        var installCommand = new InstallControlPackCommand(
                command.projectId(),
                entry.getPackId(),
                resolvedPack.resolvedVersion(),
                entry.getPublisher(),
                entry.getDescription(),
                entry.getSourceUrl(),
                resolvedPack.resolvedChecksum(),
                entry.getCompatibility(),
                entry.getRegistryMetadata(),
                command.entries());

        var result = controlPackService.install(installCommand);

        var record = new PackInstallRecord(
                project, command.packId(), entry.getPackType(), trustDecision.outcome(), InstallOutcome.INSTALLED);
        record.setRequestedVersion(command.versionConstraint());
        record.setResolvedVersion(resolvedPack.resolvedVersion());
        record.setResolvedSource(resolvedPack.resolvedSource());
        record.setResolvedChecksum(resolvedPack.resolvedChecksum());
        record.setTrustPolicyId(trustDecision.policyId());
        record.setTrustReason(trustDecision.reason());
        record.setPerformedBy(command.performedBy());
        record.setInstalledEntityId(result.controlPack().getId());

        var saved = installRecordRepository.save(record);
        log.info(
                "pack_install_completed: pack_id={}, version={}, controls_created={}",
                command.packId(),
                resolvedPack.resolvedVersion(),
                result.controlsCreated());
        return saved;
    }

    private PackInstallRecord executeUpgrade(
            InstallPackCommand command,
            com.keplerops.groundcontrol.domain.projects.model.Project project,
            com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry entry,
            ResolvedPack resolvedPack,
            TrustDecision trustDecision) {

        if (entry.getPackType() != PackType.CONTROL_PACK) {
            throw new DomainValidationException("Only CONTROL_PACK upgrade is currently supported");
        }

        if (command.entries() == null || command.entries().isEmpty()) {
            throw new DomainValidationException("Control pack entries must be provided for upgrade");
        }

        var upgradeCommand = new UpgradeControlPackCommand(
                command.projectId(),
                entry.getPackId(),
                resolvedPack.resolvedVersion(),
                entry.getPublisher(),
                entry.getDescription(),
                entry.getSourceUrl(),
                resolvedPack.resolvedChecksum(),
                entry.getCompatibility(),
                entry.getRegistryMetadata(),
                command.entries());

        var result = controlPackService.upgrade(upgradeCommand);

        var record = new PackInstallRecord(
                project, command.packId(), entry.getPackType(), trustDecision.outcome(), InstallOutcome.UPGRADED);
        record.setRequestedVersion(command.versionConstraint());
        record.setResolvedVersion(resolvedPack.resolvedVersion());
        record.setResolvedSource(resolvedPack.resolvedSource());
        record.setResolvedChecksum(resolvedPack.resolvedChecksum());
        record.setTrustPolicyId(trustDecision.policyId());
        record.setTrustReason(trustDecision.reason());
        record.setPerformedBy(command.performedBy());
        record.setInstalledEntityId(result.controlPack().getId());

        var saved = installRecordRepository.save(record);
        log.info(
                "pack_upgrade_completed: pack_id={}, version={}, previous_version={}",
                command.packId(),
                resolvedPack.resolvedVersion(),
                result.previousVersion());
        return saved;
    }
}
