package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportPdfService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import com.keplerops.groundcontrol.domain.documents.service.RequirementExportData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentExportPdfServiceTest {

    private final DocumentExportPdfService service = new DocumentExportPdfService();

    @Test
    void emptyDocument_producesValidPdf() {
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test Doc", "1.0", "", List.of());
        byte[] pdf = service.toPdf(order, Map.of());
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 5, StandardCharsets.UTF_8)).startsWith("%PDF");
    }

    @Test
    void sectionWithRequirement_producesLargerOutput() {
        var emptyOrder = new DocumentReadingOrder(UUID.randomUUID(), "Empty", "1.0", "", List.of());
        byte[] emptyPdf = service.toPdf(emptyOrder, Map.of());

        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-001", "Title", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section 1", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "With Req", "1.0", "Description", List.of(section));
        var reqs = Map.of("REQ-001", new RequirementExportData("REQ-001", "Title", "Statement.", "", List.of()));
        byte[] reqPdf = service.toPdf(order, reqs);

        assertThat(reqPdf.length).isGreaterThan(emptyPdf.length);
    }

    @Test
    void textBlockAndRelations_produceValidPdf() {
        var textItem = new ReadingOrderContentItem("TEXT_BLOCK", null, null, "Some text content.", 0);
        var reqItem = new ReadingOrderContentItem("REQUIREMENT", "CHILD-001", "Child", null, 1);
        var section = new ReadingOrderNode(UUID.randomUUID(), "Mixed", "", 0, List.of(textItem, reqItem), List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "CHILD-001",
                new RequirementExportData("CHILD-001", "Child Req", "Statement.", "", List.of("PARENT-001")));

        byte[] pdf = service.toPdf(order, reqs);
        assertThat(new String(pdf, 0, 5, StandardCharsets.UTF_8)).startsWith("%PDF");
    }
}
