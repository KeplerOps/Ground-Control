package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackInstallResult;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackUpgradeResult;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackInstallRecordRepository;
import com.keplerops.groundcontrol.domain.packregistry.service.InstallPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.PackInstallOrchestrator;
import com.keplerops.groundcontrol.domain.packregistry.service.PackResolver;
import com.keplerops.groundcontrol.domain.packregistry.service.ResolvedPack;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustDecision;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustEvaluator;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackInstallOrchestratorTest {

    @Mock
    private PackResolver packResolver;

    @Mock
    private TrustEvaluator trustEvaluator;

    @Mock
    private PackInstallRecordRepository installRecordRepository;

    @Mock
    private ControlPackService controlPackService;

    @Mock
    private ProjectService projectService;

    private PackInstallOrchestrator orchestrator;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PACK_ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private ResolvedPack makeResolvedPack(Project project) {
        var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
        entry.setPublisher("NIST");
        entry.setSourceUrl("https://registry.example.com/nist-800-53");
        return new ResolvedPack(entry, "1.0.0", "https://registry.example.com/nist-800-53", "sha256:abc123", List.of());
    }

    private List<ControlPackEntryDefinition> makeEntries() {
        return List.of(new ControlPackEntryDefinition(
                "AC-1",
                "Access Control Policy",
                null,
                null,
                ControlFunction.PREVENTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    }

    @BeforeEach
    void setUp() {
        orchestrator = new PackInstallOrchestrator(
                packResolver, trustEvaluator, installRecordRepository, controlPackService, projectService);
    }

    @Nested
    class InstallPack {

        @Test
        void resolvesAndInstallsWhenTrusted() {
            var project = makeProject();
            var resolved = makeResolvedPack(project);
            var controlPack = new ControlPack(project, "nist-800-53", "1.0.0");
            setField(controlPack, "id", PACK_ENTITY_ID);
            var installResult = new ControlPackInstallResult(controlPack, 1, 0, 1, 0, 0, false);

            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(packResolver.resolve(PROJECT_ID, "nist-800-53", "^1.0.0")).thenReturn(resolved);
            when(packResolver.checkCompatibility(resolved)).thenReturn(true);
            when(trustEvaluator.evaluate(PROJECT_ID, resolved))
                    .thenReturn(new TrustDecision(TrustOutcome.TRUSTED, "Trusted publisher", "policy-1"));
            when(controlPackService.install(any(InstallControlPackCommand.class)))
                    .thenReturn(installResult);
            when(installRecordRepository.save(any(PackInstallRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new InstallPackCommand(PROJECT_ID, "nist-800-53", "^1.0.0", "admin", makeEntries());
            var record = orchestrator.installPack(command);

            assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.INSTALLED);
            assertThat(record.getTrustOutcome()).isEqualTo(TrustOutcome.TRUSTED);
            assertThat(record.getInstalledEntityId()).isEqualTo(PACK_ENTITY_ID);
            verify(controlPackService).install(any(InstallControlPackCommand.class));
        }

        @Test
        void createsRejectionRecordWhenUntrusted() {
            var project = makeProject();
            var resolved = makeResolvedPack(project);

            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(packResolver.resolve(PROJECT_ID, "nist-800-53", null)).thenReturn(resolved);
            when(packResolver.checkCompatibility(resolved)).thenReturn(true);
            when(trustEvaluator.evaluate(PROJECT_ID, resolved))
                    .thenReturn(new TrustDecision(TrustOutcome.REJECTED, "Untrusted publisher", "policy-1"));
            when(installRecordRepository.save(any(PackInstallRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new InstallPackCommand(PROJECT_ID, "nist-800-53", null, "admin", makeEntries());
            var record = orchestrator.installPack(command);

            assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.REJECTED);
            assertThat(record.getTrustOutcome()).isEqualTo(TrustOutcome.REJECTED);
            verify(controlPackService, never()).install(any());
        }

        @Test
        void recordsFailureWhenResolutionFails() {
            var project = makeProject();
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(packResolver.resolve(PROJECT_ID, "missing-pack", null)).thenThrow(new NotFoundException("Not found"));
            when(installRecordRepository.save(any(PackInstallRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new InstallPackCommand(PROJECT_ID, "missing-pack", null, "admin", makeEntries());
            var record = orchestrator.installPack(command);

            assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.FAILED);
            assertThat(record.getErrorDetail()).contains("Resolution failed");
            verify(controlPackService, never()).install(any());
        }

        @Test
        void rejectsIncompatiblePack() {
            var project = makeProject();
            var resolved = makeResolvedPack(project);

            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(packResolver.resolve(PROJECT_ID, "nist-800-53", null)).thenReturn(resolved);
            when(packResolver.checkCompatibility(resolved)).thenReturn(false);
            when(installRecordRepository.save(any(PackInstallRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new InstallPackCommand(PROJECT_ID, "nist-800-53", null, "admin", makeEntries());
            var record = orchestrator.installPack(command);

            assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.REJECTED);
            assertThat(record.getErrorDetail()).contains("not compatible");
            verify(controlPackService, never()).install(any());
        }
    }

    @Nested
    class UpgradePack {

        @Test
        void resolvesAndUpgradesWhenTrusted() {
            var project = makeProject();
            var resolved = makeResolvedPack(project);
            var controlPack = new ControlPack(project, "nist-800-53", "1.0.0");
            setField(controlPack, "id", PACK_ENTITY_ID);
            var upgradeResult = new ControlPackUpgradeResult(controlPack, "0.9.0", 1, 0, 0, 0, 0, 0);

            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(packResolver.resolve(PROJECT_ID, "nist-800-53", "^1.0.0")).thenReturn(resolved);
            when(packResolver.checkCompatibility(resolved)).thenReturn(true);
            when(trustEvaluator.evaluate(PROJECT_ID, resolved))
                    .thenReturn(new TrustDecision(TrustOutcome.TRUSTED, "Trusted", "policy-1"));
            when(controlPackService.upgrade(any(UpgradeControlPackCommand.class)))
                    .thenReturn(upgradeResult);
            when(installRecordRepository.save(any(PackInstallRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new InstallPackCommand(PROJECT_ID, "nist-800-53", "^1.0.0", "admin", makeEntries());
            var record = orchestrator.upgradePack(command);

            assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.UPGRADED);
            assertThat(record.getTrustOutcome()).isEqualTo(TrustOutcome.TRUSTED);
        }
    }
}
