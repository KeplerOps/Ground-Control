package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.model.SectionContent;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionContentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentReadingOrderService {

    private final DocumentRepository documentRepository;
    private final SectionRepository sectionRepository;
    private final SectionContentRepository contentRepository;

    public DocumentReadingOrderService(
            DocumentRepository documentRepository,
            SectionRepository sectionRepository,
            SectionContentRepository contentRepository) {
        this.documentRepository = documentRepository;
        this.sectionRepository = sectionRepository;
        this.contentRepository = contentRepository;
    }

    public DocumentReadingOrder getReadingOrder(UUID documentId) {
        var document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));

        var allSections = sectionRepository.findByDocumentIdOrderBySortOrder(documentId);

        // Batch-load all content for all sections in one query
        var sectionIds = allSections.stream().map(Section::getId).toList();
        var allContent = sectionIds.isEmpty()
                ? List.<SectionContent>of()
                : contentRepository.findBySectionIdInOrderBySortOrder(sectionIds);

        // Group content by section ID
        Map<UUID, List<SectionContent>> contentBySectionId = new LinkedHashMap<>();
        for (SectionContent c : allContent) {
            contentBySectionId
                    .computeIfAbsent(c.getSection().getId(), k -> new ArrayList<>())
                    .add(c);
        }

        // Group sections by parent ID
        Map<UUID, List<Section>> childrenByParentId = new LinkedHashMap<>();
        List<Section> roots = new ArrayList<>();
        for (Section s : allSections) {
            var parentId = s.getParent() != null ? s.getParent().getId() : null;
            if (parentId == null) {
                roots.add(s);
            } else {
                childrenByParentId
                        .computeIfAbsent(parentId, k -> new ArrayList<>())
                        .add(s);
            }
        }

        var rootNodes = roots.stream()
                .map(r -> buildNode(r, childrenByParentId, contentBySectionId))
                .toList();

        return new DocumentReadingOrder(
                document.getId(), document.getTitle(), document.getVersion(), document.getDescription(), rootNodes);
    }

    private ReadingOrderNode buildNode(
            Section section,
            Map<UUID, List<Section>> childrenByParentId,
            Map<UUID, List<SectionContent>> contentBySectionId) {
        var content = contentBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .map(c -> new ReadingOrderContentItem(
                        c.getContentType().name(),
                        c.getRequirement() != null ? c.getRequirement().getUid() : null,
                        c.getRequirement() != null ? c.getRequirement().getTitle() : null,
                        c.getTextContent(),
                        c.getSortOrder()))
                .toList();

        var children = childrenByParentId.getOrDefault(section.getId(), List.of()).stream()
                .map(child -> buildNode(child, childrenByParentId, contentBySectionId))
                .toList();

        return new ReadingOrderNode(
                section.getId(),
                section.getTitle(),
                section.getDescription(),
                section.getSortOrder(),
                content,
                children);
    }
}
