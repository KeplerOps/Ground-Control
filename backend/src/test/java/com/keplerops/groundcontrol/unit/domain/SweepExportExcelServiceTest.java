package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportExcelService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
}
