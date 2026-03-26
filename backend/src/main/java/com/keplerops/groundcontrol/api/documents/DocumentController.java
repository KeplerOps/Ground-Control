package com.keplerops.groundcontrol.api.documents;

import com.keplerops.groundcontrol.domain.documents.service.CreateDocumentCommand;
import com.keplerops.groundcontrol.domain.documents.service.DocumentReadingOrderService;
import com.keplerops.groundcontrol.domain.documents.service.DocumentService;
import com.keplerops.groundcontrol.domain.documents.service.UpdateDocumentCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentReadingOrderService readingOrderService;
    private final ProjectService projectService;

    public DocumentController(
            DocumentService documentService,
            DocumentReadingOrderService readingOrderService,
            ProjectService projectService) {
        this.documentService = documentService;
        this.readingOrderService = readingOrderService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(
            @Valid @RequestBody DocumentRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateDocumentCommand(projectId, request.title(), request.version(), request.description());
        return DocumentResponse.from(documentService.create(command));
    }

    @GetMapping
    public List<DocumentResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return documentService.listByProject(projectId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.getById(id));
    }

    @PutMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDocumentRequest request) {
        var command = new UpdateDocumentCommand(request.title(), request.version(), request.description());
        return DocumentResponse.from(documentService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }

    @GetMapping("/{id}/reading-order")
    public DocumentReadingOrderResponse readingOrder(@PathVariable UUID id) {
        return DocumentReadingOrderResponse.from(readingOrderService.getReadingOrder(id));
    }

    @PutMapping("/{id}/grammar")
    public String setGrammar(@PathVariable UUID id, @RequestBody String grammarJson) {
        documentService.setGrammar(id, grammarJson);
        return documentService.getGrammar(id);
    }

    @GetMapping("/{id}/grammar")
    public String getGrammar(@PathVariable UUID id) {
        var grammar = documentService.getGrammar(id);
        if (grammar == null) {
            return "null";
        }
        return grammar;
    }

    @DeleteMapping("/{id}/grammar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGrammar(@PathVariable UUID id) {
        documentService.deleteGrammar(id);
    }
}
