package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.model.ContentType;
import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import com.keplerops.groundcontrol.domain.documents.repository.SectionContentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SectionContentService {

    private static final Logger log = LoggerFactory.getLogger(SectionContentService.class);

    private final SectionContentRepository contentRepository;
    private final SectionRepository sectionRepository;
    private final RequirementRepository requirementRepository;

    public SectionContentService(
            SectionContentRepository contentRepository,
            SectionRepository sectionRepository,
            RequirementRepository requirementRepository) {
        this.contentRepository = contentRepository;
        this.sectionRepository = sectionRepository;
        this.requirementRepository = requirementRepository;
    }

    public SectionContent create(CreateSectionContentCommand command) {
        var section = sectionRepository
                .findById(command.sectionId())
                .orElseThrow(() -> new NotFoundException("Section not found: " + command.sectionId()));

        validateContentType(command.contentType(), command.requirementId(), command.textContent());

        var requirement = command.requirementId() != null
                ? requirementRepository
                        .findById(command.requirementId())
                        .orElseThrow(() -> new NotFoundException("Requirement not found: " + command.requirementId()))
                : null;

        var content = new SectionContent(
                section, command.contentType(), requirement, command.textContent(), command.sortOrder());
        var saved = contentRepository.save(content);
        log.info(
                "section_content_created: section={} type={} id={}",
                section.getTitle(),
                saved.getContentType(),
                saved.getId());
        return saved;
    }

    public SectionContent update(UUID id, UpdateSectionContentCommand command) {
        var content = contentRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Section content not found: " + id));

        if (command.textContent() != null && content.getContentType() == ContentType.TEXT_BLOCK) {
            content.setTextContent(command.textContent());
        }
        if (command.sortOrder() != null) {
            content.setSortOrder(command.sortOrder());
        }

        var saved = contentRepository.save(content);
        log.info("section_content_updated: id={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SectionContent> listBySection(UUID sectionId) {
        return contentRepository.findBySectionIdOrderBySortOrder(sectionId);
    }

    public void delete(UUID id) {
        var content = contentRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Section content not found: " + id));
        contentRepository.delete(content);
        log.info("section_content_deleted: id={}", content.getId());
    }

    private void validateContentType(ContentType contentType, UUID requirementId, String textContent) {
        if (contentType == ContentType.REQUIREMENT && requirementId == null) {
            throw new DomainValidationException(
                    "requirement_id is required for REQUIREMENT content type",
                    "validation_error",
                    Map.of("field", "requirementId", "contentType", contentType.name()));
        }
        if (contentType == ContentType.TEXT_BLOCK && (textContent == null || textContent.isBlank())) {
            throw new DomainValidationException(
                    "text_content is required for TEXT_BLOCK content type",
                    "validation_error",
                    Map.of("field", "textContent", "contentType", contentType.name()));
        }
        if (contentType == ContentType.REQUIREMENT && textContent != null) {
            throw new DomainValidationException(
                    "text_content must be null for REQUIREMENT content type",
                    "validation_error",
                    Map.of("field", "textContent", "contentType", contentType.name()));
        }
        if (contentType == ContentType.TEXT_BLOCK && requirementId != null) {
            throw new DomainValidationException(
                    "requirement_id must be null for TEXT_BLOCK content type",
                    "validation_error",
                    Map.of("field", "requirementId", "contentType", contentType.name()));
        }
    }
}
