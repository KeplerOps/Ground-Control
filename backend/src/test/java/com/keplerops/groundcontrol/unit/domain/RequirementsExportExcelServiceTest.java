package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData.RequirementSnapshot;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData.TraceabilityLinkSnapshot;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportExcelService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class RequirementsExportExcelServiceTest {

    private final RequirementsExportExcelService service = new RequirementsExportExcelService();

    @Test
    void toExcel_producesValidXlsxBytes() {
        var data = new RequirementsExportData("test-project", Instant.now(), List.of());
        byte[] bytes = service.toExcel(data);
        // XLSX files are ZIP archives, which start with PK (0x50, 0x4B)
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    void toExcel_hasTwoSheets() throws IOException {
        var data = new RequirementsExportData("test-project", Instant.now(), List.of());
        byte[] bytes = service.toExcel(data);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Requirements");
            assertThat(workbook.getSheetName(1)).isEqualTo("Traceability");
        }
    }

    @Test
    void toExcel_requirementDataAppearsInSheet() throws IOException {
        var links = List.of(new TraceabilityLinkSnapshot("CODE_FILE", "Main.java", "IMPLEMENTS", "", ""));
        var req = new RequirementSnapshot(
                "GC-001",
                "Test",
                "Statement",
                "Rationale",
                "FUNCTIONAL",
                "MUST",
                "DRAFT",
                1,
                links,
                Instant.now(),
                Instant.now());
        var data = new RequirementsExportData("test-project", Instant.now(), List.of(req));

        byte[] bytes = service.toExcel(data);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Requirements");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("GC-001");

            var traceSheet = workbook.getSheet("Traceability");
            assertThat(traceSheet.getLastRowNum()).isEqualTo(1);
            assertThat(traceSheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("GC-001");
        }
    }
}
