package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportReqifService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import com.keplerops.groundcontrol.domain.documents.service.RequirementExportData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentExportReqifServiceTest {

    private final DocumentExportReqifService service = new DocumentExportReqifService();

    @Test
    void emptyDocument_producesValidReqifXml() {
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test Doc", "1.0", "", List.of());
        String reqif = service.toReqif(order, Map.of());
        assertThat(reqif).startsWith("<?xml");
        assertThat(reqif).contains("<REQ-IF");
        assertThat(reqif).contains("</REQ-IF>");
        assertThat(reqif).contains("<TITLE>Test Doc</TITLE>");
    }

    @Test
    void requirementWithStatement_emitsSpecObject() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-001", "Login", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs =
                Map.of("REQ-001", new RequirementExportData("REQ-001", "Login", "Must provide login.", "", List.of()));

        String reqif = service.toReqif(order, reqs);

        assertThat(reqif).contains("SPEC-OBJECT");
        assertThat(reqif).contains("IDENTIFIER=\"REQ-001\"");
        assertThat(reqif).contains("LONG-NAME=\"Login\"");
        assertThat(reqif).contains("Must provide login.");
        assertThat(reqif).contains("SPEC-HIERARCHY");
        assertThat(reqif).contains("OBJECT-REF");
    }

    @Test
    void parentRelation_emitsSpecRelation() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "CHILD-001", "Child", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "CHILD-001", new RequirementExportData("CHILD-001", "Child", "Statement.", "", List.of("PARENT-001")));

        String reqif = service.toReqif(order, reqs);

        assertThat(reqif).contains("SPEC-RELATION");
        assertThat(reqif).contains("<SOURCE-REF>CHILD-001</SOURCE-REF>");
        assertThat(reqif).contains("<TARGET-REF>PARENT-001</TARGET-REF>");
        assertThat(reqif).contains("srt-parent");
    }

    @Test
    void xmlSpecialCharacters_areEscaped() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-XSS", "Title", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of("REQ-XSS", new RequirementExportData("REQ-XSS", "A & B", "x < y > z", "", List.of()));

        String reqif = service.toReqif(order, reqs);

        assertThat(reqif).contains("A &amp; B");
        assertThat(reqif).contains("x &lt; y &gt; z");
        assertThat(reqif).doesNotContain("<y>");
    }

    @Test
    void roundTrip_exportThenParse_preservesRequirements() {
        var content = List.of(
                new ReadingOrderContentItem("REQUIREMENT", "RT-001", "First", null, 0),
                new ReadingOrderContentItem("REQUIREMENT", "RT-002", "Second", null, 1));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "RT-001", new RequirementExportData("RT-001", "First", "Statement one.", "", List.of()),
                "RT-002", new RequirementExportData("RT-002", "Second", "Statement two.", "", List.of("RT-001")));

        String reqif = service.toReqif(order, reqs);

        // Parse back with ReqifParser
        var parsed = com.keplerops.groundcontrol.domain.requirements.service.ReqifParser.parse(reqif);
        assertThat(parsed.requirements()).hasSize(2);
        assertThat(parsed.requirements().stream().map(r -> r.identifier()))
                .containsExactlyInAnyOrder("RT-001", "RT-002");
        assertThat(parsed.relations()).hasSize(1);
        assertThat(parsed.relations().get(0).sourceIdentifier()).isEqualTo("RT-002");
        assertThat(parsed.relations().get(0).targetIdentifier()).isEqualTo("RT-001");
    }
}
