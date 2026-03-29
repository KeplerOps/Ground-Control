package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportHtmlService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentExportSdocService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentExportService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrder;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrderService;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderContentItem;
import com.keplerops.groundcontrol.domain.documents.service.ReadingOrderNode;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRelationRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceTest {

    @Mock
    private DocumentReadingOrderService readingOrderService;

    @Mock
    private DocumentExportSdocService sdocService;

    @Mock
    private DocumentExportHtmlService htmlService;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementRelationRepository relationRepository;

    private DocumentExportService service;

    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID REQ_ID = UUID.randomUUID();
    private static final UUID PARENT_REQ_ID = UUID.randomUUID();
    private static final Project TEST_PROJECT = new Project("test", "Test");

    @BeforeEach
    void setUp() {
        service = new DocumentExportService(
                readingOrderService, sdocService, htmlService, requirementRepository, relationRepository);
    }

    @Test
    void exportToSdoc_delegatesToSdocService() {
        var order = new DocumentReadingOrder(DOC_ID, "Test Doc", "1.0", "", List.of());
        when(readingOrderService.getReadingOrder(DOC_ID)).thenReturn(order);
        when(sdocService.toSdoc(eq(order), any())).thenReturn("[[SECTION]]\n");

        String result = service.exportToSdoc(DOC_ID);

        assertThat(result).isEqualTo("[[SECTION]]\n");
        verify(readingOrderService).getReadingOrder(DOC_ID);
        verify(sdocService).toSdoc(eq(order), any());
    }

    @Test
    void exportToSdoc_collectsRequirementUidsAndFetchesData() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "REQ-001", "Title", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(DOC_ID, "Test Doc", "1.0", "", List.of(section));
        when(readingOrderService.getReadingOrder(DOC_ID)).thenReturn(order);

        var req = makeRequirement("REQ-001", REQ_ID);
        when(requirementRepository.findByUid("REQ-001")).thenReturn(Optional.of(req));
        when(relationRepository.findBySourceId(REQ_ID)).thenReturn(List.of());
        when(sdocService.toSdoc(any(), any())).thenReturn("");

        service.exportToSdoc(DOC_ID);

        verify(requirementRepository).findByUid("REQ-001");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportToSdoc_includesParentRelations() {
        var content = List.of(new ReadingOrderContentItem("REQUIREMENT", "CHILD-001", "Child", null, 0));
        var section = new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, content, List.of());
        var order = new DocumentReadingOrder(DOC_ID, "Test Doc", "1.0", "", List.of(section));
        when(readingOrderService.getReadingOrder(DOC_ID)).thenReturn(order);

        var childReq = makeRequirement("CHILD-001", REQ_ID);
        var parentReq = makeRequirement("PARENT-001", PARENT_REQ_ID);
        when(requirementRepository.findByUid("CHILD-001")).thenReturn(Optional.of(childReq));

        var relation = new RequirementRelation(childReq, parentReq, RelationType.PARENT);
        when(relationRepository.findBySourceId(REQ_ID)).thenReturn(List.of(relation));

        var captor = ArgumentCaptor.forClass(Map.class);
        when(sdocService.toSdoc(any(), captor.capture())).thenReturn("");

        service.exportToSdoc(DOC_ID);

        Map<String, ?> reqMap = captor.getValue();
        assertThat(reqMap).containsKey("CHILD-001");
    }

    @Test
    void exportToSdoc_skipsTextBlocksInUidCollection() {
        var textContent = new ReadingOrderContentItem("TEXT_BLOCK", null, null, "Some text", 0);
        var reqContent = new ReadingOrderContentItem("REQUIREMENT", "REQ-002", "Title", null, 1);
        var section =
                new ReadingOrderNode(UUID.randomUUID(), "Section", "", 0, List.of(textContent, reqContent), List.of());
        var order = new DocumentReadingOrder(DOC_ID, "Test Doc", "1.0", "", List.of(section));
        when(readingOrderService.getReadingOrder(DOC_ID)).thenReturn(order);

        var req = makeRequirement("REQ-002", REQ_ID);
        when(requirementRepository.findByUid("REQ-002")).thenReturn(Optional.of(req));
        when(relationRepository.findBySourceId(REQ_ID)).thenReturn(List.of());
        when(sdocService.toSdoc(any(), any())).thenReturn("");

        service.exportToSdoc(DOC_ID);

        // Only REQ-002 should be fetched, not the text block
        verify(requirementRepository).findByUid("REQ-002");
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
