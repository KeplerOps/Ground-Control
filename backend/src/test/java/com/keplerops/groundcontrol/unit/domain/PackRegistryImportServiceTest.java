package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.packregistry.service.ControlPackRegistrationContent;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportFormat;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportOptions;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportService;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryService;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PackRegistryImportServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final PackRegistryImportService service =
            new PackRegistryImportService(new ObjectMapper().findAndRegisterModules(), mock(PackRegistryService.class));

    @Test
    void convertsOscalCatalogIntoControlPackRegisterCommand() {
        var json =
                """
                {
                  "catalog": {
                    "uuid": "11111111-1111-1111-1111-111111111111",
                    "metadata": {
                      "title": "NIST SP 800-53 Rev. 5",
                      "version": "5.1.0",
                      "oscal-version": "1.1.2",
                      "links": [{"href": "https://example.test/nist.json"}],
                      "parties": [{"type": "organization", "name": "NIST"}]
                    },
                    "groups": [{
                      "id": "ac",
                      "title": "Access Control",
                      "controls": [{
                        "id": "ac-1",
                        "title": "Policy and Procedures",
                        "props": [{"name": "label", "value": "AC-1"}],
                        "parts": [
                          {"name": "statement", "parts": [{"name": "item", "prose": "Develop and publish policy."}]},
                          {"name": "guidance", "prose": "Tailor to local conditions."}
                        ],
                        "controls": [{
                          "id": "ac-2.1",
                          "title": "Automated Account Management",
                          "props": [{"name": "label", "value": "AC-2 (1)"}],
                          "parts": [{"name": "statement", "prose": "Support automated account management."}]
                        }]
                      }]
                    }]
                  }
                }
                """;

        var command = service.toRegisterCommand(
                PROJECT_ID,
                "nist.json",
                json.getBytes(StandardCharsets.UTF_8),
                new PackRegistryImportOptions(
                        PackRegistryImportFormat.OSCAL_JSON,
                        "nist-sp800-53-rev5",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("importedBy", "test"),
                        null,
                        ControlFunction.DETECTIVE));

        assertThat(command.packId()).isEqualTo("nist-sp800-53-rev5");
        assertThat(command.packType()).isEqualTo(PackType.CONTROL_PACK);
        assertThat(command.version()).isEqualTo("5.1.0");
        assertThat(command.publisher()).isEqualTo("NIST");
        assertThat(command.provenance()).containsEntry("importedBy", "test");
        assertThat(command.registryMetadata()).containsEntry("importedControlCount", 2);
        var content = (ControlPackRegistrationContent) command.registrationContent();
        assertThat(content.entries()).hasSize(2);
        assertThat(content.entries().getFirst().uid()).isEqualTo("AC-1");
        assertThat(content.entries().getFirst().controlFunction()).isEqualTo(ControlFunction.DETECTIVE);
        assertThat(content.entries().getFirst().category()).isEqualTo("Access Control");
        assertThat(content.entries().getFirst().implementationGuidance()).contains("Tailor to local conditions.");
        assertThat(content.entries().get(1).uid()).isEqualTo("AC-2 (1)");
    }

    @Test
    void manifestImportHonorsOverrides() {
        var json =
                """
                {
                  "packId": "upstream-pack",
                  "packType": "CONTROL_PACK",
                  "version": "1.0.0",
                  "publisher": "Upstream",
                  "controlPackEntries": [{
                    "uid": "AC-1",
                    "title": "Policy and Procedures",
                    "controlFunction": "PREVENTIVE",
                    "description": "Original description"
                  }]
                }
                """;

        var command = service.toRegisterCommand(
                PROJECT_ID,
                "manifest.json",
                json.getBytes(StandardCharsets.UTF_8),
                new PackRegistryImportOptions(
                        PackRegistryImportFormat.GC_MANIFEST,
                        "override-pack",
                        "2.0.0",
                        "Override Publisher",
                        null,
                        "https://example.test/source.json",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(command.packId()).isEqualTo("override-pack");
        assertThat(command.version()).isEqualTo("2.0.0");
        assertThat(command.publisher()).isEqualTo("Override Publisher");
        assertThat(command.sourceUrl()).isEqualTo("https://example.test/source.json");
        var content = (ControlPackRegistrationContent) command.registrationContent();
        assertThat(content.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.uid()).isEqualTo("AC-1");
            assertThat(entry.controlFunction()).isEqualTo(ControlFunction.PREVENTIVE);
        });
    }
}
