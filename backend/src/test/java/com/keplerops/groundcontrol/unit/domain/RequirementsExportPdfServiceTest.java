package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportData.RequirementSnapshot;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementsExportPdfService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsExportPdfServiceTest {

    private final RequirementsExportPdfService service = new RequirementsExportPdfService();

    @Test
    void toPdf_producesValidPdfBytes() {
        var data = new RequirementsExportData("test-project", Instant.now(), List.of());
        byte[] bytes = service.toPdf(data);
        assertThat(bytes.length).isGreaterThan(0);
        // PDF files start with %PDF
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.UTF_8))
                .startsWith("%PDF");
    }

    @Test
    void toPdf_withRequirements_producesLargerOutput() {
        var req = new RequirementSnapshot(
                "GC-001",
                "Test",
                "Statement text",
                "Rationale",
                "FUNCTIONAL",
                "MUST",
                "ACTIVE",
                1,
                List.of(),
                Instant.now(),
                Instant.now());
        var dataEmpty = new RequirementsExportData("test-project", Instant.now(), List.of());
        var dataWithReq = new RequirementsExportData("test-project", Instant.now(), List.of(req));
        byte[] emptyPdf = service.toPdf(dataEmpty);
        byte[] reqPdf = service.toPdf(dataWithReq);
        assertThat(reqPdf.length).isGreaterThan(emptyPdf.length);
    }
}
