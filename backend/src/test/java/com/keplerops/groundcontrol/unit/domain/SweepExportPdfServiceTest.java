package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportPdfService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SweepExportPdfServiceTest {

    private final SweepExportPdfService service = new SweepExportPdfService();

    @Test
    void toPdf_producesValidPdfBytes() {
        var report = emptyReport();
        byte[] bytes = service.toPdf(report);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 5)).startsWith("%PDF");
    }

    @Test
    void toPdf_withOrphans_producesLargerOutput() {
        var emptyBytes = service.toPdf(emptyReport());
        var orphan = new SweepReport.RequirementSummary("GC-001", "Orphan");
        var reportWithOrphan = new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(orphan),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(1, Map.of("DRAFT", 1), List.of()),
                null);
        var orphanBytes = service.toPdf(reportWithOrphan);
        assertThat(orphanBytes.length).isGreaterThan(emptyBytes.length);
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
