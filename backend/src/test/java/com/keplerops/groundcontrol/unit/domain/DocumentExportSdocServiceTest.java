package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportSdocService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import com.keplerops.groundcontrol.domain.documents.service.RequirementExportData;
import com.keplerops.groundcontrol.domain.requirements.service.SdocDocument;
import com.keplerops.groundcontrol.domain.requirements.service.SdocParser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentExportSdocServiceTest {

    private final DocumentExportSdocService service = new DocumentExportSdocService();

    @Test
    void emptyDocument_producesEmptyOutput() {
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of());
        String sdoc = service.toSdoc(order, Map.of());
        assertThat(sdoc).isEmpty();
    }

    @Test
    void sectionWithRequirement_producesValidSdoc() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-001", "Test Req", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Wave 1 — Foundation", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs =
                Map.of("REQ-001", new RequirementExportData("REQ-001", "Test Req", "Statement text.", "", List.of()));

        String sdoc = service.toSdoc(order, reqs);

        assertThat(sdoc).contains("[[SECTION]]");
        assertThat(sdoc).contains("TITLE: Wave 1 — Foundation");
        assertThat(sdoc).contains("[REQUIREMENT]");
        assertThat(sdoc).contains("UID: REQ-001");
        assertThat(sdoc).contains("TITLE: Test Req");
        assertThat(sdoc).contains("STATEMENT: >>>\nStatement text.\n<<<");
        assertThat(sdoc).contains("[[/SECTION]]");
    }

    @Test
    void requirementWithParentRelations_emitsRelationsBlock() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "CHILD-001", "Child", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "CHILD-001",
                new RequirementExportData("CHILD-001", "Child", "Statement.", "", List.of("PARENT-001", "PARENT-002")));

        String sdoc = service.toSdoc(order, reqs);

        assertThat(sdoc).contains("RELATIONS:");
        assertThat(sdoc).contains("- TYPE: Parent\n  VALUE: PARENT-001");
        assertThat(sdoc).contains("- TYPE: Parent\n  VALUE: PARENT-002");
    }

    @Test
    void textBlock_emitsTextMarker() {
        var content = List.of(new ReadingOrderContentItem("TEXT_BLOCK", null, null, "Some descriptive text.", 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));

        String sdoc = service.toSdoc(order, Map.of());

        assertThat(sdoc).contains("[TEXT]\nSome descriptive text.");
    }

    @Test
    void roundTrip_exportThenParse_preservesStructure() {
        var reqContent = new ReadingOrderContentItem("REQUIREMENT", "RT-001", "Round Trip", null, 0);
        var textContent = new ReadingOrderContentItem("TEXT_BLOCK", null, null, "Intro text.", 0);
        var reqContent2 = new ReadingOrderContentItem("REQUIREMENT", "RT-002", "Second", null, 1);
        var section = new ReadingOrderNode(
                UUID.randomUUID(), "Wave 1 — Testing", "", 0, List.of(textContent, reqContent, reqContent2), List.of());
        var order = new DocumentReadingOrder(UUID.randomUUID(), "Test", "1.0", "", List.of(section));
        var reqs = Map.of(
                "RT-001", new RequirementExportData("RT-001", "Round Trip", "First statement.", "", List.of()),
                "RT-002", new RequirementExportData("RT-002", "Second", "Second statement.", "", List.of("RT-001")));

        String sdoc = service.toSdoc(order, reqs);

        // Parse it back
        SdocDocument parsed = SdocParser.parse(sdoc);
        assertThat(parsed.requirements()).hasSize(2);
        assertThat(parsed.requirements().get(0).uid()).isEqualTo("RT-001");
        assertThat(parsed.requirements().get(1).uid()).isEqualTo("RT-002");
        assertThat(parsed.requirements().get(1).parentUids()).containsExactly("RT-001");
        assertThat(parsed.sections()).hasSize(1);
        assertThat(parsed.sections().get(0).title()).isEqualTo("Wave 1 — Testing");
        assertThat(parsed.sections().get(0).wave()).isEqualTo(1);
        assertThat(parsed.sections().get(0).items()).hasSize(3);
    }
}
