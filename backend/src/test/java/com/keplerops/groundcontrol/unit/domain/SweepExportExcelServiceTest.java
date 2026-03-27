package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessIssue;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportExcelService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SweepExportExcelServiceTest {

    private final SweepExportExcelService service = new SweepExportExcelService();

    @Test
    void toExcel_producesValidXlsx() {
        var report = emptyReport();
        byte[] bytes = service.toExcel(report);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    void toExcel_hasExpectedSheetNames() throws IOException {
        var report = emptyReport();
        byte[] bytes = service.toExcel(report);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(7);
            assertThat(workbook.getSheetName(0)).isEqualTo("Summary");
            assertThat(workbook.getSheetName(1)).isEqualTo("Cycles");
            assertThat(workbook.getSheetName(2)).isEqualTo("Orphans");
            assertThat(workbook.getSheetName(3)).isEqualTo("Coverage Gaps");
            assertThat(workbook.getSheetName(4)).isEqualTo("Violations");
            assertThat(workbook.getSheetName(5)).isEqualTo("Completeness");
            assertThat(workbook.getSheetName(6)).isEqualTo("Quality Gates");
        }
    }

    @Test
    void toExcel_withFullData_populatesAllSheets() throws IOException {
        var report = fullReport();
        byte[] bytes = service.toExcel(report);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            // Summary has header + 1 data row
            assertThat(workbook.getSheet("Summary").getLastRowNum()).isEqualTo(1);
            // Cycles has header + 1 edge row
            assertThat(workbook.getSheet("Cycles").getLastRowNum()).isEqualTo(1);
            // Orphans has header + 1 data row
            assertThat(workbook.getSheet("Orphans").getLastRowNum()).isEqualTo(1);
            // Coverage Gaps has header + 1 data row
            assertThat(workbook.getSheet("Coverage Gaps").getLastRowNum()).isEqualTo(1);
            // Violations: 1 cross-wave + 1 consistency
            assertThat(workbook.getSheet("Violations").getLastRowNum()).isEqualTo(2);
            // Completeness: header + 2 status rows + blank + "Issues" label + 1 issue
            assertThat(workbook.getSheet("Completeness").getLastRowNum()).isGreaterThan(2);
            // Quality Gates has header + 1 gate row
            assertThat(workbook.getSheet("Quality Gates").getLastRowNum()).isEqualTo(1);
        }
    }

    private SweepReport emptyReport() {
        return new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(0, Map.of(), List.of()),
                null);
    }

    private SweepReport fullReport() {
        var cycle = new CycleResult(
                List.of("GC-001", "GC-002"), List.of(new CycleEdge("GC-001", "GC-002", RelationType.DEPENDS_ON)));
        var orphan = new SweepReport.RequirementSummary("GC-003", "Orphan Req");
        var coverageGap = Map.of("IMPLEMENTS", List.of(new SweepReport.RequirementSummary("GC-004", "Uncovered")));
        var crossWave = List.of(new SweepReport.CrossWaveViolationSummary("GC-005", 1, "GC-006", 2, "DEPENDS_ON"));
        var consistency = List.of(
                new SweepReport.ConsistencyViolationSummary("GC-007", "ACTIVE", "GC-008", "ACTIVE", "ACTIVE_CONFLICT"));
        var completeness = new CompletenessResult(
                5, Map.of("DRAFT", 3, "ACTIVE", 2), List.of(new CompletenessIssue("GC-009", "missing statement")));
        var gate = new QualityGateResult(
                UUID.randomUUID(), "Coverage Gate", "COVERAGE", "IMPLEMENTS", null, "GTE", 80.0, 60.0, false);
        var qgResult = new QualityGateEvaluationResult("test-project", Instant.now(), false, 1, 0, 1, List.of(gate));

        return new SweepReport(
                "test-project",
                Instant.now(),
                List.of(cycle),
                List.of(orphan),
                coverageGap,
                crossWave,
                consistency,
                completeness,
                qgResult);
    }
}
