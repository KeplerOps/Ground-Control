package com.keplerops.groundcontrol.domain.packregistry.service;

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
public class PackInstallOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PackInstallOrchestrator.class);

    private static String sanitizeForLog(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\r\\n\\t]", "_");
    }

    private final PackResolver packResolver;
    private final PackIntegrityVerifier packIntegrityVerifier;
    private final TrustEvaluator trustEvaluator;
    private final PackInstallRecordRepository installRecordRepository;
    private final PackInstallRecordWriter installRecordWriter;
    private final PackTypeHandlerRegistry packTypeHandlerRegistry;
    private final ProjectService projectService;

    public PackInstallOrchestrator(
            PackResolver packResolver,
            PackIntegrityVerifier packIntegrityVerifier,
            TrustEvaluator trustEvaluator,
            PackInstallRecordRepository installRecordRepository,
            PackInstallRecordWriter installRecordWriter,
            PackTypeHandlerRegistry packTypeHandlerRegistry,
            ProjectService projectService) {
        this.packResolver = packResolver;
        this.packIntegrityVerifier = packIntegrityVerifier;
        this.trustEvaluator = trustEvaluator;
        this.installRecordRepository = installRecordRepository;
        this.installRecordWriter = installRecordWriter;
        this.packTypeHandlerRegistry = packTypeHandlerRegistry;
        this.projectService = projectService;
    }

    public PackInstallRecord installPack(InstallPackCommand command) {
        return execute(command, PackOperation.INSTALL);
    }

    public PackInstallRecord upgradePack(InstallPackCommand command) {
        return execute(command, PackOperation.UPGRADE);
    }

    private PackInstallRecord execute(InstallPackCommand command, PackOperation operation) {
        var project = projectService.getById(command.projectId());
        log.info(
                "pack_{}_orchestration_started: pack_id={}, version_constraint={}",
                operation.logLabel(),
                sanitizeForLog(command.packId()),
                sanitizeForLog(command.versionConstraint()));

        var gate = resolveAndEvaluateTrust(command, project, operation);
        if (gate.rejectionRecord() != null) {
            return gate.rejectionRecord();
        }

        try {
            var handler = packTypeHandlerRegistry.get(gate.entry().getPackType());
            var result = operation.apply(
                    handler,
                    new PackOperationContext(command.projectId(), gate.entry(), gate.resolved(), gate.integrity()));
            var record = buildRecord(
                    project,
                    command,
                    gate.entry(),
                    gate.resolved(),
                    gate.integrity(),
                    gate.trust(),
                    operation.successOutcome());
            record.setInstalledEntityId(result.installedEntityId());
            var saved = installRecordWriter.save(record);
            log.info(
                    "pack_{}_completed: pack_id={}, version={}, installed_entity_id={}",
                    operation.logLabel(),
                    gate.entry().getPackId(),
                    gate.resolved().resolvedVersion(),
                    result.installedEntityId());
            return saved;
        } catch (Exception e) {
            var record = buildRecord(
                    project,
                    command,
                    gate.entry(),
                    gate.resolved(),
                    gate.integrity(),
                    gate.trust(),
                    InstallOutcome.FAILED);
            record.setErrorDetail(operation.failurePrefix() + e.getMessage());
            log.info(
                    "pack_{}_failed: pack_id={}, error={}",
                    operation.logLabel(),
                    gate.entry().getPackId(),
                    e.getMessage());
            return installRecordWriter.save(record);
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
            ResolvedPack resolved,
            PackRegistryEntry entry,
            PackIntegrityVerification integrity,
            TrustDecision trust,
            PackInstallRecord rejectionRecord) {}

    private enum PackOperation {
        INSTALL("install", InstallOutcome.INSTALLED, "Installation failed: ") {
            @Override
            PackOperationResult apply(PackTypeHandler handler, PackOperationContext context) {
                return handler.install(context);
            }
        },
        UPGRADE("upgrade", InstallOutcome.UPGRADED, "Upgrade failed: ") {
            @Override
            PackOperationResult apply(PackTypeHandler handler, PackOperationContext context) {
                return handler.upgrade(context);
            }
        };

        private final String logLabel;
        private final InstallOutcome successOutcome;
        private final String failurePrefix;

        PackOperation(String logLabel, InstallOutcome successOutcome, String failurePrefix) {
            this.logLabel = logLabel;
            this.successOutcome = successOutcome;
            this.failurePrefix = failurePrefix;
        }

        String logLabel() {
            return logLabel;
        }

        InstallOutcome successOutcome() {
            return successOutcome;
        }

        String failurePrefix() {
            return failurePrefix;
        }

        abstract PackOperationResult apply(PackTypeHandler handler, PackOperationContext context);
    }

    private GateResult resolveAndEvaluateTrust(InstallPackCommand command, Project project, PackOperation operation) {
        ResolvedPack resolvedPack;
        try {
            resolvedPack = packResolver.resolve(command.projectId(), command.packId(), command.versionConstraint());
        } catch (NotFoundException e) {
            var record = new PackInstallRecord(
                    project, command.packId(), PackType.UNKNOWN, TrustOutcome.UNKNOWN, InstallOutcome.FAILED);
            record.setRequestedVersion(command.versionConstraint());
            record.setPerformedBy(command.performedBy());
            record.setErrorDetail("Resolution failed: " + e.getMessage());
            log.info(
                    "pack_{}_failed: pack_id={}, reason=resolution_failed",
                    operation.logLabel(),
                    sanitizeForLog(command.packId()));
            return new GateResult(null, null, null, null, installRecordWriter.save(record));
        }

        var entry = resolvedPack.entry();

        if (!packResolver.checkCompatibility(resolvedPack)) {
            var trust = new TrustDecision(TrustOutcome.UNKNOWN, null, null);
            var record = buildRecord(project, command, entry, resolvedPack, null, trust, InstallOutcome.REJECTED);
            record.setErrorDetail("Pack is not compatible with the current platform version");
            log.info("pack_{}_rejected: pack_id={}, reason=incompatible", operation.logLabel(), entry.getPackId());
            return new GateResult(resolvedPack, entry, null, trust, installRecordWriter.save(record));
        }

        PackIntegrityVerification integrityVerification;
        try {
            integrityVerification = packIntegrityVerifier.verify(resolvedPack);
        } catch (PackIntegrityException exception) {
            var trust = new TrustDecision(TrustOutcome.REJECTED, exception.getMessage(), null);
            var integrity = new PackIntegrityVerification(
                    exception.getVerifiedChecksum(),
                    exception.isChecksumVerified(),
                    exception.getSignatureVerified(),
                    exception.getSignerTrusted());
            var record = buildRecord(project, command, entry, resolvedPack, integrity, trust, InstallOutcome.REJECTED);
            record.setErrorDetail(exception.getMessage());
            log.info("pack_{}_rejected: pack_id={}, reason=integrity_failed", operation.logLabel(), entry.getPackId());
            return new GateResult(resolvedPack, entry, integrity, trust, installRecordWriter.save(record));
        }

        var trustDecision = trustEvaluator.evaluate(command.projectId(), resolvedPack, integrityVerification);
        if (trustDecision.outcome() == TrustOutcome.REJECTED) {
            var record = buildRecord(
                    project,
                    command,
                    entry,
                    resolvedPack,
                    integrityVerification,
                    trustDecision,
                    InstallOutcome.REJECTED);
            log.info(
                    "pack_{}_trust_rejected: pack_id={}, policy={}",
                    operation.logLabel(),
                    entry.getPackId(),
                    trustDecision.policyId());
            return new GateResult(
                    resolvedPack, entry, integrityVerification, trustDecision, installRecordWriter.save(record));
        }

        return new GateResult(resolvedPack, entry, integrityVerification, trustDecision, null);
    }

    private PackInstallRecord buildRecord(
            Project project,
            InstallPackCommand command,
            PackRegistryEntry entry,
            ResolvedPack resolvedPack,
            PackIntegrityVerification integrityVerification,
            TrustDecision trustDecision,
            InstallOutcome outcome) {
        var record =
                new PackInstallRecord(project, command.packId(), entry.getPackType(), trustDecision.outcome(), outcome);
        record.setRequestedVersion(command.versionConstraint());
        record.setResolvedVersion(resolvedPack.resolvedVersion());
        record.setResolvedSource(resolvedPack.resolvedSource());
        record.setResolvedChecksum(
                integrityVerification != null
                        ? integrityVerification.verifiedChecksum()
                        : resolvedPack.resolvedChecksum());
        record.setSignatureVerified(integrityVerification != null ? integrityVerification.signatureVerified() : null);
        record.setTrustPolicyId(trustDecision.policyId());
        record.setTrustReason(trustDecision.reason());
        record.setPerformedBy(command.performedBy());
        return record;
    }
}
