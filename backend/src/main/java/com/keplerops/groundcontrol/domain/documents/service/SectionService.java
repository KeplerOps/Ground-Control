package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.model.Section;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.documents.repository.SectionRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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

        // Iterative bottom-up assembly: a recursive descent on a deeply
        // nested section tree would risk StackOverflowError. Materialise
        // nodes in descending-depth order so each parent finds its
        // children's SectionTreeNode instances already built (codex
        // cycle-1 class finding category — same shape as
        // TestCaseFolderService.getTree).
        return buildIterativeForest(all, roots, childrenByParentId);
    }

    public void delete(UUID id) {
        var section =
                sectionRepository.findById(id).orElseThrow(() -> new NotFoundException("Section not found: " + id));
        sectionRepository.delete(section);
        log.info("section_deleted: id={} title={}", section.getId(), section.getTitle());
    }

    /**
     * Iterative O(n) section tree assembly (codex cycle-2 finding).
     * Walks the children map from each root via an explicit stack to
     * produce a post-order visit list, then materialises SectionTreeNode
     * instances in that order so each parent finds its children's nodes
     * already built. Each section is touched twice (push + pop); no
     * per-node parent walk is performed, so a linear chain stays linear
     * rather than quadratic.
     */
    private List<SectionTreeNode> buildIterativeForest(
            List<Section> all, List<Section> roots, Map<UUID, List<Section>> childrenByParentId) {
        List<Section> postOrder = new ArrayList<>(all.size());
        HashSet<UUID> visited = new HashSet<>();
        Deque<SectionStackFrame> stack = new ArrayDeque<>();
        for (Section root : roots) {
            stack.push(new SectionStackFrame(root));
            while (!stack.isEmpty()) {
                SectionStackFrame frame = stack.peek();
                if (frame.expanded) {
                    stack.pop();
                    postOrder.add(frame.section);
                    continue;
                }
                if (!visited.add(frame.section.getId())) {
                    stack.pop();
                    continue;
                }
                frame.expanded = true;
                List<Section> children = childrenByParentId.getOrDefault(frame.section.getId(), List.of());
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(new SectionStackFrame(children.get(i)));
                }
            }
        }

        Map<UUID, SectionTreeNode> nodesById = new HashMap<>();
        for (Section section : postOrder) {
            var childSections = childrenByParentId.getOrDefault(section.getId(), List.of());
            List<SectionTreeNode> children = new ArrayList<>(childSections.size());
            for (Section child : childSections) {
                children.add(nodesById.get(child.getId()));
            }
            nodesById.put(
                    section.getId(),
                    new SectionTreeNode(
                            section.getId(),
                            section.getParent() != null ? section.getParent().getId() : null,
                            section.getTitle(),
                            section.getDescription(),
                            section.getSortOrder(),
                            section.getCreatedAt(),
                            section.getUpdatedAt(),
                            children));
        }

        List<SectionTreeNode> result = new ArrayList<>(roots.size());
        for (Section root : roots) {
            result.add(nodesById.get(root.getId()));
        }
        return result;
    }

    /** Stack frame used by the iterative post-order traversal in {@link #buildIterativeForest}. */
    private static final class SectionStackFrame {
        final Section section;
        boolean expanded;

        SectionStackFrame(Section section) {
            this.section = section;
        }
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
