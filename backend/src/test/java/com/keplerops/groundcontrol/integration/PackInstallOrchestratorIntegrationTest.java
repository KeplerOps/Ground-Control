package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.model.RegisteredControlPackEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackInstallRecordRepository;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackRegistryEntryRepository;
import com.keplerops.groundcontrol.domain.packregistry.service.InstallPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.PackInstallOrchestrator;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PackInstallOrchestratorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ControlPackService controlPackService;

    @Autowired
    private PackRegistryEntryRepository packRegistryEntryRepository;

    @Autowired
    private PackInstallRecordRepository packInstallRecordRepository;

    @Autowired
    private PackInstallOrchestrator packInstallOrchestrator;

    @Test
    void upgradeConflictPersistsFailureRecord() {
        var project = projectRepository.findByIdentifier("ground-control").orElseThrow();
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        var packId = "e2e-pack-" + suffix;
        var controlUid = "E2E-" + suffix.toUpperCase(Locale.ROOT);

        var controlEntry = new ControlPackEntryDefinition(
                controlUid,
                "Rollback Test Control",
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
                null);

        controlPackService.install(new InstallControlPackCommand(
                project.getId(),
                packId,
                "1.0.0",
                "NIST",
                "Rollback test pack",
                "https://registry.example.com/" + packId,
                null,
                null,
                null,
                List.of(controlEntry)));

        var registryEntry = new PackRegistryEntry(project, packId, PackType.CONTROL_PACK, "1.0.0");
        registryEntry.setPublisher("NIST");
        registryEntry.setSourceUrl("https://registry.example.com/" + packId);
        registryEntry.setControlPackEntries(List.of(new RegisteredControlPackEntry(
                controlUid,
                "Rollback Test Control",
                ControlFunction.PREVENTIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)));
        packRegistryEntryRepository.save(registryEntry);

        var record = packInstallOrchestrator.upgradePack(
                new InstallPackCommand(project.getId(), packId, "1.0.0", "integration-test"));

        assertThat(record.getInstallOutcome()).isEqualTo(InstallOutcome.FAILED);
        assertThat(record.getErrorDetail()).contains("already at version 1.0.0");
        assertThat(packInstallRecordRepository.findByProjectIdAndPackIdOrderByPerformedAtDesc(project.getId(), packId))
                .hasSize(1)
                .allSatisfy(saved -> {
                    assertThat(saved.getInstallOutcome()).isEqualTo(InstallOutcome.FAILED);
                    assertThat(saved.getErrorDetail()).contains("already at version 1.0.0");
                });
    }
}
