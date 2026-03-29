package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportHtmlService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import com.keplerops.groundcontrol.domain.documents.service.RequirementExportData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentExportHtmlServiceTest {

    private final DocumentExportHtmlService service = new DocumentExportHtmlService();

    @Test
    void emptyDocument_producesMinimalHtml() {
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test Doc", "1.0", "", List.of());
        String html = service.toHtml(order, Map.of());
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("<title>Test Doc</title>");
        assertThat(html).contains("<h1>Test Doc</h1>");
        assertThat(html).contains("</html>");
    }

    @Test
    void sectionWithRequirement_rendersUidTitleStatement() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-001", "Test Req", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Foundation", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of("REQ-001", new RequirementExportData("REQ-001", "Test Req", "The statement.", "", List.of()));

        String html = service.toHtml(order, reqs);

        assertThat(html).contains("<h2>Foundation</h2>");
        assertThat(html).contains("REQ-001");
        assertThat(html).contains("Test Req");
        assertThat(html).contains("The statement.");
    }

    @Test
    void textBlock_renderedAsDiv() {
        var content = List.of(new ReadingOrderContentItem("TEXT_BLOCK", null, null, "Some descriptive text.", 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));

        String html = service.toHtml(order, Map.of());

        assertThat(html).contains("class=\"text-block\"");
        assertThat(html).contains("Some descriptive text.");
    }

    @Test
    void specialCharacters_areHtmlEscaped() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-XSS", "Title", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "REQ-XSS",
                new RequirementExportData("REQ-XSS", "<script>alert('xss')</script>", "a < b && c > d", "", List.of()));

        String html = service.toHtml(order, reqs);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("a &lt; b &amp;&amp; c &gt; d");
    }

    @Test
    void requirementWithComment_rendersCommentDiv() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-C01", "With Comment", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "REQ-C01",
                new RequirementExportData(
                        "REQ-C01", "With Comment", "Statement.", "Safety-critical rationale.", List.of()));

        String html = service.toHtml(order, reqs);

        assertThat(html).contains("class=\"comment\"");
        assertThat(html).contains("Safety-critical rationale.");
    }

    @Test
    void requirementWithEmptyComment_omitsCommentDiv() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-C02", "No Comment", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs =
                Map.of("REQ-C02", new RequirementExportData("REQ-C02", "No Comment", "Statement.", "", List.of()));

        String html = service.toHtml(order, reqs);

        assertThat(html).doesNotContain("class=\"comment\"");
    }

    @Test
    void parentRelations_renderedInRequirement() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "CHILD-001", "Child", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "CHILD-001",
                new RequirementExportData("CHILD-001", "Child", "Statement.", "", List.of("PARENT-001", "PARENT-002")));

        String html = service.toHtml(order, reqs);

        assertThat(html).contains("Parent:");
        assertThat(html).contains("PARENT-001");
        assertThat(html).contains("PARENT-002");
    }
}
