package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportCsvService;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData.RequirementSnapshot;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData.TraceabilityLinkSnapshot;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsExportCsvServiceTest {

    private final RequirementsExportCsvService service = new RequirementsExportCsvService();

    @Test
    void toCsv_emptyList_returnsHeaderOnly() {
        var data = new RequirementsExportData("test-project", Instant.now(), List.of());
        var csv = service.toCsv(data);
        var lines = csv.split("\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).startsWith("uid,title,");
    }

    @Test
    void toCsv_singleRequirement_formatsCorrectly() {
        var req = new RequirementSnapshot(
                "GC-001",
                "Test Req",
                "Shall do X",
                "Because Y",
                "FUNCTIONAL",
                "MUST",
                "DRAFT",
                1,
                List.of(),
                Instant.parse("2026-03-26T00:00:00Z"),
                Instant.parse("2026-03-26T00:00:00Z"));
        var data = new RequirementsExportData("test-project", Instant.now(), List.of(req));
        var csv = service.toCsv(data);
        var lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1]).contains("GC-001");
        assertThat(lines[1]).contains("FUNCTIONAL");
        assertThat(lines[1]).contains("MUST");
        assertThat(lines[1]).contains("DRAFT");
    }

    @Test
    void toCsv_withTraceabilityLinks_formatsSemicolonSeparated() {
        var links = List.of(
                new TraceabilityLinkSnapshot("CODE_FILE", "src/Main.java", "IMPLEMENTS", "", ""),
                new TraceabilityLinkSnapshot("TEST", "src/MainTest.java", "TESTS", "", ""));
        var req = new RequirementSnapshot(
                "GC-002",
                "Linked Req",
                "Shall do Z",
                "",
                "FUNCTIONAL",
                "SHOULD",
                "ACTIVE",
                2,
                links,
                Instant.now(),
                Instant.now());
        var data = new RequirementsExportData("test-project", Instant.now(), List.of(req));
        var csv = service.toCsv(data);
        assertThat(csv).contains("IMPLEMENTS:src/Main.java; TESTS:src/MainTest.java");
    }

    @Test
    void toCsv_formulaInjection_isPrevented() {
        var req = new RequirementSnapshot(
                "GC-003",
                "=CMD('calc')",
                "Statement",
                "",
                "FUNCTIONAL",
                "MUST",
                "DRAFT",
                null,
                List.of(),
                Instant.now(),
                Instant.now());
        var data = new RequirementsExportData("test-project", Instant.now(), List.of(req));
        var csv = service.toCsv(data);
        assertThat(csv).doesNotContain(",=CMD");
    }
}
