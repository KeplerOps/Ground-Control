package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.documents.service.CreateSectionCommand;
import com.keplerops.groundcontrol.domain.documents.service.SectionService;
import com.keplerops.groundcontrol.domain.documents.service.UpdateSectionCommand;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private DocumentRepository documentRepository;

    private SectionService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Document TEST_DOC = createTestDocument();

    private static Document createTestDocument() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        var doc = new Document(project, "SRS", "1.0.0", null, null);
        setField(doc, "id", DOC_ID);
        return doc;
    }

    @BeforeEach
    void setUp() {
        service = new SectionService(sectionRepository, documentRepository);
    }

    @Nested
    class Create {

        @Test
        void createsRootSection() {
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(TEST_DOC));
            when(sectionRepository.existsByDocumentIdAndParentIdIsNullAndTitle(DOC_ID, "Chapter 1"))
                    .thenReturn(false);
            when(sectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateSectionCommand(DOC_ID, null, "Chapter 1", "First chapter", 0);
            var result = service.create(command);

            assertThat(result.getTitle()).isEqualTo("Chapter 1");
            assertThat(result.getParent()).isNull();
            verify(sectionRepository).save(any(Section.class));
        }

        @Test
        void createsChildSection() {
            var parent = makeSection("Chapter 1", null, 0);
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(TEST_DOC));
            when(sectionRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
            when(sectionRepository.existsByDocumentIdAndParentIdAndTitle(DOC_ID, parent.getId(), "Section 1.1"))
                    .thenReturn(false);
            when(sectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateSectionCommand(DOC_ID, parent.getId(), "Section 1.1", null, 0);
            var result = service.create(command);

            assertThat(result.getTitle()).isEqualTo("Section 1.1");
            assertThat(result.getParent()).isEqualTo(parent);
        }

        @Test
        void throwsConflictOnDuplicateTitle() {
            when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(TEST_DOC));
            when(sectionRepository.existsByDocumentIdAndParentIdIsNullAndTitle(DOC_ID, "Chapter 1"))
                    .thenReturn(true);

            var command = new CreateSectionCommand(DOC_ID, null, "Chapter 1", null, 0);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesFields() {
            var section = makeSection("Old Title", null, 0);
            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
            when(sectionRepository.existsByDocumentIdAndParentIdIsNullAndTitle(DOC_ID, "New Title"))
                    .thenReturn(false);
            when(sectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateSectionCommand("New Title", "Updated", 5);
            var result = service.update(section.getId(), command);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getDescription()).isEqualTo("Updated");
            assertThat(result.getSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    class GetAndList {

        @Test
        void getByIdReturnsSection() {
            var section = makeSection("Chapter 1", null, 0);
            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));

            assertThat(service.getById(section.getId()).getTitle()).isEqualTo("Chapter 1");
        }

        @Test
        void getByIdThrowsWhenNotFound() {
            var id = UUID.randomUUID();
            when(sectionRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByDocumentDelegates() {
            when(sectionRepository.findByDocumentIdOrderBySortOrder(DOC_ID)).thenReturn(List.of());
            assertThat(service.listByDocument(DOC_ID)).isEmpty();
        }
    }

    @Nested
    class Tree {

        @Test
        void buildsNestedTree() {
            var root = makeSection("Chapter 1", null, 0);
            var child = makeSection("Section 1.1", root, 0);
            var grandchild = makeSection("Section 1.1.1", child, 0);

            when(sectionRepository.findByDocumentIdOrderBySortOrder(DOC_ID))
                    .thenReturn(List.of(root, child, grandchild));

            var tree = service.getTree(DOC_ID);

            assertThat(tree).hasSize(1);
            assertThat(tree.getFirst().title()).isEqualTo("Chapter 1");
            assertThat(tree.getFirst().children()).hasSize(1);
            assertThat(tree.getFirst().children().getFirst().title()).isEqualTo("Section 1.1");
            assertThat(tree.getFirst().children().getFirst().children()).hasSize(1);
            assertThat(tree.getFirst()
                            .children()
                            .getFirst()
                            .children()
                            .getFirst()
                            .title())
                    .isEqualTo("Section 1.1.1");
        }

        @Test
        void emptyDocumentReturnsEmptyTree() {
            when(sectionRepository.findByDocumentIdOrderBySortOrder(DOC_ID)).thenReturn(List.of());
            assertThat(service.getTree(DOC_ID)).isEmpty();
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesSection() {
            var section = makeSection("Chapter 1", null, 0);
            when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));

            service.delete(section.getId());
            verify(sectionRepository).delete(section);
        }
    }

    private static Section makeSection(String title, Section parent, int sortOrder) {
        var section = new Section(TEST_DOC, parent, title, null, sortOrder);
        setField(section, "id", UUID.randomUUID());
        setField(section, "createdAt", Instant.now());
        setField(section, "updatedAt", Instant.now());
        return section;
    }
}
