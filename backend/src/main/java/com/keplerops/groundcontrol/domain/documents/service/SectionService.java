package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SectionService {

    private static final Logger log = LoggerFactory.getLogger(SectionService.class);

    private final SectionRepository sectionRepository;
    private final DocumentRepository documentRepository;

    public SectionService(SectionRepository sectionRepository, DocumentRepository documentRepository) {
        this.sectionRepository = sectionRepository;
        this.documentRepository = documentRepository;
    }

    public Section create(CreateSectionCommand command) {
        var document = documentRepository
                .findById(command.documentId())
                .orElseThrow(() -> new NotFoundException("Document not found: " + command.documentId()));

        Section parent = null;
        if (command.parentId() != null) {
            parent = sectionRepository
                    .findById(command.parentId())
                    .orElseThrow(() -> new NotFoundException("Parent section not found: " + command.parentId()));
        }

        checkTitleUniqueness(command.documentId(), command.parentId(), command.title());

        var section = new Section(document, parent, command.title(), command.description(), command.sortOrder());
        var saved = sectionRepository.save(section);
        log.info(
                "section_created: document={} title={} parentId={} id={}",
                document.getTitle(),
                saved.getTitle(),
                command.parentId(),
                saved.getId());
        return saved;
    }

    public Section update(UUID id, UpdateSectionCommand command) {
        var section =
                sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("Section not found: " + id));

        if (command.title() != null) {
            if (!command.title().equals(section.getTitle())) {
                var parentId = section.getParent() != null ? section.getParent().getId() : null;
                checkTitleUniqueness(section.getDocument().getId(), parentId, command.title());
            }
            section.setTitle(command.title());
        }
        if (command.description() != null) {
            section.setDescription(command.description());
        }
        if (command.sortOrder() != null) {
            section.setSortOrder(command.sortOrder());
        }

        var saved = sectionRepository.save(section);
        log.info("section_updated: id={} title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public Section getById(UUID id) {
        return sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("Section not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Section> listByDocument(UUID documentId) {
        return sectionRepository.findByDocumentIdOrderBySortOrder(documentId);
    }

    @Transactional(readOnly = true)
    public List<SectionTreeNode> getTree(UUID documentId) {
        var all = sectionRepository.findByDocumentIdOrderBySortOrder(documentId);

        Map<UUID, List<Section>> childrenByParentId = new LinkedHashMap<>();
        List<Section> roots = new ArrayList<>();

        for (Section s : all) {
            var parentId = s.getParent() != null ? s.getParent().getId() : null;
            if (parentId == null) {
                roots.add(s);
            } else {
                childrenByParentId
                        .computeIfAbsent(parentId, k -> new ArrayList<>())
                        .add(s);
            }
        }

        return roots.stream().map(r -> buildNode(r, childrenByParentId)).toList();
    }

    public void delete(UUID id) {
        var section =
                sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("Section not found: " + id));
        sectionRepository.delete(section);
        log.info("section_deleted: id={} title={}", section.getId(), section.getTitle());
    }

    private SectionTreeNode buildNode(Section section, Map<UUID, List<Section>> childrenByParentId) {
        var children = childrenByParentId.getOrDefault(section.getId(), List.of());
        return new SectionTreeNode(
                section.getId(),
                section.getParent() != null ? section.getParent().getId() : null,
                section.getTitle(),
                section.getDescription(),
                section.getSortOrder(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                children.stream().map(c -> buildNode(c, childrenByParentId)).toList());
    }

    private void checkTitleUniqueness(UUID documentId, UUID parentId, String title) {
        boolean exists;
        if (parentId == null) {
            exists = sectionRepository.existsByDocumentIdAndParentIdIsNullAndTitle(documentId, title);
        } else {
            exists = sectionRepository.existsByDocumentIdAndParentIdAndTitle(documentId, parentId, title);
        }
        if (exists) {
            throw new ConflictException("Section with title '" + title + "' already exists at this level");
        }
    }
}
