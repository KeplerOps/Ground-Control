package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.model.ContentType;
import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionContentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrderService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentReadingOrderServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionContentRepository contentRepository;

    private DocumentReadingOrderService service;

    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createProject();
    private static final Document TEST_DOC = createDocument();

    private static Project createProject() {
        var p = new Project("test", "Test");
        setField(p, "id", UUID.fromString("00000000-0000-0000-0000-000000000099"));
        return p;
    }

    private static Document createDocument() {
        var d = new Document(TEST_PROJECT, "SRS", "1.0", "System Requirements", null);
        setField(d, "id", DOC_ID);
        return d;
    }

    @BeforeEach
    void setUp() {
        service = new DocumentReadingOrderService(documentRepository, sectionRepository, contentRepository);
    }

    @Test
    void emptyDocumentReturnsEmptySections() {
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(TEST_DOC));
        when(sectionRepository.findByDocumentIdOrderBySortOrder(DOC_ID)).thenReturn(List.of());

        var result = service.getReadingOrder(DOC_ID);

        assertThat(result.documentId()).isEqualTo(DOC_ID);
        assertThat(result.title()).isEqualTo("SRS");
        assertThat(result.sections()).isEmpty();
    }

    @Test
    void buildsNestedSectionsWithContent() {
        var root = makeSection("Chapter 1", null, 0);
        var child = makeSection("Section 1.1", root, 0);

        var req = new Requirement(TEST_PROJECT, "GC-001", "Req Title", "Statement");
        setField(req, "id", UUID.randomUUID());
        var reqContent = new SectionContent(root, ContentType.REQUIREMENT, req, null, 0);
        setField(reqContent, "id", UUID.randomUUID());
        setField(reqContent, "createdAt", Instant.now());
        setField(reqContent, "updatedAt", Instant.now());

        var textContent = new SectionContent(child, ContentType.TEXT_BLOCK, null, "Some text", 0);
        setField(textContent, "id", UUID.randomUUID());
        setField(textContent, "createdAt", Instant.now());
        setField(textContent, "updatedAt", Instant.now());

        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(TEST_DOC));
        when(sectionRepository.findByDocumentIdOrderBySortOrder(DOC_ID)).thenReturn(List.of(root, child));
        when(contentRepository.findBySectionIdInOrderBySortOrder(List.of(root.getId(), child.getId())))
                .thenReturn(List.of(reqContent, textContent));

        var result = service.getReadingOrder(DOC_ID);

        assertThat(result.sections()).hasSize(1);
        var rootNode = result.sections().getFirst();
        assertThat(rootNode.title()).isEqualTo("Chapter 1");
        assertThat(rootNode.content()).hasSize(1);
        assertThat(rootNode.content().getFirst().contentType()).isEqualTo("REQUIREMENT");
        assertThat(rootNode.content().getFirst().requirementUid()).isEqualTo("GC-001");

        assertThat(rootNode.children()).hasSize(1);
        var childNode = rootNode.children().getFirst();
        assertThat(childNode.title()).isEqualTo("Section 1.1");
        assertThat(childNode.content()).hasSize(1);
        assertThat(childNode.content().getFirst().contentType()).isEqualTo("TEXT_BLOCK");
        assertThat(childNode.content().getFirst().textContent()).isEqualTo("Some text");
    }

    private static Section makeSection(String title, Section parent, int sortOrder) {
        var s = new Section(TEST_DOC, parent, title, null, sortOrder);
        setField(s, "id", UUID.randomUUID());
        setField(s, "createdAt", Instant.now());
        setField(s, "updatedAt", Instant.now());
        return s;
    }
}
