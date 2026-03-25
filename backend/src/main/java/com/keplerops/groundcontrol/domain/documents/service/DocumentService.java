package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.documents.model.Document;
import com.keplerops.groundcontrol.domain.documents.repository.DocumentRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ProjectService projectService;

    public DocumentService(DocumentRepository documentRepository, ProjectService projectService) {
        this.documentRepository = documentRepository;
        this.projectService = projectService;
    }

    public Document create(CreateDocumentCommand command) {
        var project = projectService.getById(command.projectId());

        if (documentRepository.existsByProjectIdAndTitle(project.getId(), command.title())) {
            throw new ConflictException("Document with title '" + command.title() + "' already exists in project "
                    + project.getIdentifier());
        }

        var doc = new Document(project, command.title(), command.version(), command.description(), ActorHolder.get());
        var saved = documentRepository.save(doc);
        log.info(
                "document_created: project={} title={} version={} id={}",
                project.getIdentifier(),
                saved.getTitle(),
                saved.getVersion(),
                saved.getId());
        return saved;
    }

    public Document update(UUID id, UpdateDocumentCommand command) {
        var doc = documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found: " + id));

        if (command.title() != null) {
            if (!command.title().equals(doc.getTitle())
                    && documentRepository.existsByProjectIdAndTitle(
                            doc.getProject().getId(), command.title())) {
                throw new ConflictException("Document with title '" + command.title() + "' already exists in project "
                        + doc.getProject().getIdentifier());
            }
            doc.setTitle(command.title());
        }
        if (command.version() != null) {
            doc.setVersion(command.version());
        }
        if (command.descriptionProvided()) {
            doc.setDescription(command.description().orElse(null));
        }

        var saved = documentRepository.save(doc);
        log.info("document_updated: id={} title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public Document getById(UUID id) {
        return documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> listByProject(UUID projectId) {
        return documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public void delete(UUID id) {
        var doc = documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found: " + id));
        documentRepository.delete(doc);
        log.info("document_deleted: id={} title={}", doc.getId(), doc.getTitle());
    }
}
