package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.packregistry.service.ControlPackRegistrationContent;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportFormat;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportOptions;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryImportService;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryService;
import com.keplerops.groundcontrol.domain.packregistry.service.RegisterPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Test
    void importEntryDelegatesRegistrationToRegistryService() {
        var registryService = mock(PackRegistryService.class);
        var importService = new PackRegistryImportService(new ObjectMapper().findAndRegisterModules(), registryService);
        var expected = mock(com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry.class);
        when(registryService.registerEntry(any(RegisterPackCommand.class))).thenReturn(expected);

        var result = importService.importEntry(
                PROJECT_ID,
                "manifest.json",
                """
                {
                  "packId": "demo-pack",
                  "packType": "CONTROL_PACK",
                  "version": "1.0.0",
                  "controlPackEntries": [{"uid": "AC-1", "title": "Policy"}]
                }
                """
                        .getBytes(StandardCharsets.UTF_8),
                new PackRegistryImportOptions(
                        PackRegistryImportFormat.AUTO,
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
                        null,
                        null));

        var commandCaptor = ArgumentCaptor.forClass(RegisterPackCommand.class);
        verify(registryService).registerEntry(commandCaptor.capture());
        assertThat(commandCaptor.getValue().packId()).isEqualTo("demo-pack");
        assertThat(result).isSameAs(expected);
    }

    @Test
    void autoDetectImportRejectsUnknownJsonShape() {
        var json = "{\"hello\":\"world\"}";
        var options = defaultOptions(PackRegistryImportFormat.AUTO);

        assertThatThrownBy(() -> toRegisterCommand("unknown.json", json, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Could not detect import format");
    }

    @Test
    void rejectsInvalidJsonInput() {
        var json = "{not-json}";
        var options = defaultOptions(PackRegistryImportFormat.AUTO);

        assertThatThrownBy(() -> toRegisterCommand("broken.json", json, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Import file must be valid JSON");
    }

    @Test
    void oscalImportUsesFilenameFallbackSlugAndNormalizesWhitespace() {
        var json =
                """
                {
                  "catalog": {
                    "metadata": {
                      "version": "2026.1",
                      "links": [{"href": "https://example.test/catalog.json"}]
                    },
                    "controls": [{
                      "id": "ac-1",
                      "title": "  Policy\\r\\n  Rules  ",
                      "parts": [
                        {"name": "statement", "prose": "Line one   \\n"},
                        {"name": "statement", "parts": [{"name": "item", "prose": "Line two\\r\\n"}]}
                      ]
                    }]
                  }
                }
                """;

        var command = service.toRegisterCommand(
                PROJECT_ID,
                "Uber Catalog.json",
                json.getBytes(StandardCharsets.UTF_8),
                new PackRegistryImportOptions(
                        PackRegistryImportFormat.OSCAL_JSON,
                        null,
                        null,
                        "NIST",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(command.packId()).isEqualTo("uber-catalog");
        assertThat(command.sourceUrl()).isEqualTo("https://example.test/catalog.json");
        var content = (ControlPackRegistrationContent) command.registrationContent();
        assertThat(content.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.uid()).isEqualTo("AC-1");
            assertThat(entry.title()).isEqualTo("Policy\n  Rules");
            assertThat(entry.description()).isEqualTo("Line one\n\nLine two");
        });
    }

    @Test
    void manifestImportParsesDependenciesAndNestedEntryMetadata() {
        var json =
                """
                {
                  "packId": "source-pack",
                  "packType": "CONTROL_PACK",
                  "version": "1.0.0",
                  "dependencies": [{"packId": "base-pack", "versionConstraint": "^2.0.0"}],
                  "controlPackEntries": [{
                    "uid": "AC-1",
                    "title": "Policy",
                    "owner": "Security",
                    "implementationScope": "Global",
                    "methodologyFactors": {"strength": "high"},
                    "effectiveness": {"score": 0.95},
                    "expectedEvidence": [{"type": "doc"}],
                    "frameworkMappings": [{"framework": "NIST", "identifier": "AC-1"}]
                  }]
                }
                """;

        var command = service.toRegisterCommand(
                PROJECT_ID,
                "manifest.json",
                json.getBytes(StandardCharsets.UTF_8),
                new PackRegistryImportOptions(
                        PackRegistryImportFormat.GC_MANIFEST,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("minVersion", "1.0.0"),
                        null,
                        null,
                        null,
                        ControlFunction.DETECTIVE));

        assertThat(command.dependencies()).singleElement().satisfies(dep -> {
            assertThat(dep.packId()).isEqualTo("base-pack");
            assertThat(dep.versionConstraint()).isEqualTo("^2.0.0");
        });
        assertThat(command.compatibility()).containsEntry("minVersion", "1.0.0");
        var content = (ControlPackRegistrationContent) command.registrationContent();
        assertThat(content.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.controlFunction()).isEqualTo(ControlFunction.DETECTIVE);
            assertThat(entry.owner()).isEqualTo("Security");
            assertThat(entry.implementationScope()).isEqualTo("Global");
            assertThat(entry.methodologyFactors()).containsEntry("strength", "high");
            assertThat(entry.effectiveness()).containsEntry("score", 0.95);
            assertThat(entry.expectedEvidence()).hasSize(1);
            assertThat(entry.frameworkMappings()).hasSize(1);
        });
    }

    @Test
    void oscalImportRejectsMissingVersionAndEmptyControls() {
        var missingVersionJson =
                """
                {
                  "catalog": {
                    "metadata": {"title": "NIST"},
                    "controls": [{"id": "ac-1", "title": "Policy"}]
                  }
                }
                """;
        var noControlsJson =
                """
                {
                  "catalog": {
                    "metadata": {"title": "NIST", "version": "1.0.0"},
                    "groups": []
                  }
                }
                """;
        var options = defaultOptions(PackRegistryImportFormat.OSCAL_JSON);

        assertThatThrownBy(() -> toRegisterCommand("missing-version.json", missingVersionJson, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("missing metadata.version");

        assertThatThrownBy(() -> toRegisterCommand("no-controls.json", noControlsJson, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("does not contain any controls");
    }

    @Test
    void manifestImportRejectsInvalidEntryAndDependencyShapes() {
        var badEntriesJson =
                """
                {
                  "packId": "source-pack",
                  "packType": "CONTROL_PACK",
                  "version": "1.0.0",
                  "controlPackEntries": {"uid": "AC-1"}
                }
                """;
        var badDependencyJson =
                """
                {
                  "packId": "source-pack",
                  "packType": "CONTROL_PACK",
                  "version": "1.0.0",
                  "dependencies": [{"versionConstraint": "^1.0.0"}]
                }
                """;
        var options = defaultOptions(PackRegistryImportFormat.GC_MANIFEST);

        assertThatThrownBy(() -> toRegisterCommand("bad-entries.json", badEntriesJson, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("controlPackEntries must be an array");

        assertThatThrownBy(() -> toRegisterCommand("bad-deps.json", badDependencyJson, options))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Each dependency must include packId");
    }

    private RegisterPackCommand toRegisterCommand(String filename, String json, PackRegistryImportOptions options) {
        return service.toRegisterCommand(PROJECT_ID, filename, json.getBytes(StandardCharsets.UTF_8), options);
    }

    private PackRegistryImportOptions defaultOptions(PackRegistryImportFormat format) {
        return new PackRegistryImportOptions(
                format, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
