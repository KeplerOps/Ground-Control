package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.documents.model.ContentType;
import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import com.keplerops.groundcontrol.domain.documents.repository.SectionContentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.documents.service.CreateSectionContentCommand;
import com.keplerops.groundcontrol.domain.documents.service.SectionContentService;
import com.keplerops.groundcontrol.domain.documents.service.UpdateSectionContentCommand;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
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
class SectionContentServiceTest {

    @Mock
    private SectionContentRepository contentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private RequirementRepository requirementRepository;

    private SectionContentService service;

    private static final UUID SECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID REQ_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final Section TEST_SECTION = createTestSection();
    private static final Requirement TEST_REQ = createTestRequirement();

    private static Section createTestSection() {
        var project = new Project("test", "Test");
        setField(project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var doc = new Document(project, "SRS", "1.0", null, null);
        setField(doc, "id", UUID.fromString("00000000-0000-0000-0000-000000000002"));
        var section = new Section(doc, null, "Chapter 1", null, 0);
        setField(section, "id", SECTION_ID);
        return section;
    }

    private static Requirement createTestRequirement() {
        var project = new Project("test", "Test");
        setField(project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var req = new Requirement(project, "GC-001", "Title", "Statement");
        setField(req, "id", REQ_ID);
        return req;
    }

    @BeforeEach
    void setUp() {
        service = new SectionContentService(contentRepository, sectionRepository, requirementRepository);
    }

    @Nested
    class Create {

        @Test
        void addsRequirementReference() {
            when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(TEST_SECTION));
            when(requirementRepository.findById(REQ_ID)).thenReturn(Optional.of(TEST_REQ));
            when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateSectionContentCommand(SECTION_ID, ContentType.REQUIREMENT, REQ_ID, null, 0);
            var result = service.create(command);

            assertThat(result.getContentType()).isEqualTo(ContentType.REQUIREMENT);
            assertThat(result.getRequirement()).isEqualTo(TEST_REQ);
            verify(contentRepository).save(any(SectionContent.class));
        }

        @Test
        void addsTextBlock() {
            when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(TEST_SECTION));
            when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateSectionContentCommand(SECTION_ID, ContentType.TEXT_BLOCK, null, "Some text", 1);
            var result = service.create(command);

            assertThat(result.getContentType()).isEqualTo(ContentType.TEXT_BLOCK);
            assertThat(result.getTextContent()).isEqualTo("Some text");
        }

        @Test
        void rejectsRequirementWithoutId() {
            when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(TEST_SECTION));

            var command = new CreateSectionContentCommand(SECTION_ID, ContentType.REQUIREMENT, null, null, 0);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsTextBlockWithoutContent() {
            when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(TEST_SECTION));

            var command = new CreateSectionContentCommand(SECTION_ID, ContentType.TEXT_BLOCK, null, null, 0);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(DomainValidationException.class);
        }

        @Test
        void rejectsMixedFields() {
            when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(TEST_SECTION));

            var command = new CreateSectionContentCommand(SECTION_ID, ContentType.REQUIREMENT, REQ_ID, "text", 0);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class ListAndUpdate {

        @Test
        void listsInOrder() {
            when(contentRepository.findBySectionIdOrderBySortOrder(SECTION_ID)).thenReturn(List.of());
            assertThat(service.listBySection(SECTION_ID)).isEmpty();
        }

        @Test
        void updatesTextAndSortOrder() {
            var content = makeContent(ContentType.TEXT_BLOCK, null, "old text", 0);
            when(contentRepository.findById(content.getId())).thenReturn(Optional.of(content));
            when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateSectionContentCommand("new text", 5);
            var result = service.update(content.getId(), command);

            assertThat(result.getTextContent()).isEqualTo("new text");
            assertThat(result.getSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesContent() {
            var content = makeContent(ContentType.TEXT_BLOCK, null, "text", 0);
            when(contentRepository.findById(content.getId())).thenReturn(Optional.of(content));

            service.delete(content.getId());
            verify(contentRepository).delete(content);
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(contentRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
        }
    }

    private static SectionContent makeContent(ContentType type, Requirement requirement, String text, int sortOrder) {
        var content = new SectionContent(TEST_SECTION, type, requirement, text, sortOrder);
        setField(content, "id", UUID.randomUUID());
        setField(content, "createdAt", Instant.now());
        setField(content, "updatedAt", Instant.now());
        return content;
    }
}
