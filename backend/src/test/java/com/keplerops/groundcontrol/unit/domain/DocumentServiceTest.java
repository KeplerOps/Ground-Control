package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.service.CreateDocumentCommand;
import com.keplerops.groundcontrol.domain.documents.service.DocumentService;
import com.keplerops.groundcontrol.domain.documents.service.UpdateDocumentCommand;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.lang.reflect.Field;
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
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectService projectService;

    private DocumentService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentRepository, projectService);
    }

    @Nested
    class Create {

        @Test
        void createsDocumentSuccessfully() {
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);
            when(documentRepository.existsByProjectIdAndTitle(PROJECT_ID, "SRS"))
                    .thenReturn(false);
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateDocumentCommand(PROJECT_ID, "SRS", "1.0.0", "System Requirements Spec");
            var result = service.create(command);

            assertThat(result.getTitle()).isEqualTo("SRS");
            assertThat(result.getVersion()).isEqualTo("1.0.0");
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        void throwsConflictOnDuplicateTitle() {
            when(projectService.getById(PROJECT_ID)).thenReturn(TEST_PROJECT);
            when(documentRepository.existsByProjectIdAndTitle(PROJECT_ID, "SRS"))
                    .thenReturn(true);

            var command = new CreateDocumentCommand(PROJECT_ID, "SRS", "1.0.0", null);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesFields() {
            var doc = makeDocument("Old Title", "1.0.0");
            when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
            when(documentRepository.existsByProjectIdAndTitle(PROJECT_ID, "New Title"))
                    .thenReturn(false);
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = UpdateDocumentCommand.of("New Title", "2.0.0", Optional.of("Updated"));
            var result = service.update(doc.getId(), command);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getVersion()).isEqualTo("2.0.0");
            assertThat(result.getDescription()).isEqualTo("Updated");
        }

        @Test
        void throwsConflictOnDuplicateTitle() {
            var doc = makeDocument("Old Title", "1.0.0");
            when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
            when(documentRepository.existsByProjectIdAndTitle(PROJECT_ID, "Taken Title"))
                    .thenReturn(true);

            var command = UpdateDocumentCommand.of("Taken Title", null, null);
            assertThatThrownBy(() -> service.update(doc.getId(), command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class GetAndList {

        @Test
        void getByIdReturnsDocument() {
            var doc = makeDocument("SRS", "1.0.0");
            when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

            var result = service.getById(doc.getId());
            assertThat(result.getTitle()).isEqualTo("SRS");
        }

        @Test
        void getByIdThrowsWhenNotFound() {
            var id = UUID.randomUUID();
            when(documentRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByProjectDelegates() {
            when(documentRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID))
                    .thenReturn(List.of());
            assertThat(service.listByProject(PROJECT_ID)).isEmpty();
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesDocument() {
            var doc = makeDocument("SRS", "1.0.0");
            when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

            service.delete(doc.getId());
            verify(documentRepository).delete(doc);
        }
    }

    private static Document makeDocument(String title, String version) {
        var doc = new Document(TEST_PROJECT, title, version, null, null);
        setField(doc, "id", UUID.randomUUID());
        setField(doc, "createdAt", Instant.now());
        setField(doc, "updatedAt", Instant.now());
        return doc;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
