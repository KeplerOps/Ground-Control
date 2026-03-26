package com.keplerops.groundcontrol.api.sections;

import com.keplerops.groundcontrol.domain.documents.service.CreateSectionCommand;
import com.keplerops.groundcontrol.domain.documents.service.SectionService;
import com.keplerops.groundcontrol.domain.documents.service.UpdateSectionCommand;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping("/documents/{documentId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse create(@PathVariable UUID documentId, @Valid @RequestBody SectionRequest request) {
        var command = new CreateSectionCommand(
                documentId,
                request.parentId(),
                request.title(),
                request.description(),
                request.sortOrder() != null ? request.sortOrder() : 0);
        return SectionResponse.from(sectionService.create(command));
    }

    @GetMapping("/documents/{documentId}/sections")
    public List<SectionResponse> list(@PathVariable UUID documentId) {
        return sectionService.listByDocument(documentId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @GetMapping("/documents/{documentId}/sections/tree")
    public List<SectionTreeResponse> tree(@PathVariable UUID documentId) {
        return sectionService.getTree(documentId).stream()
                .map(SectionTreeResponse::from)
                .toList();
    }

    @GetMapping("/sections/{id}")
    public SectionResponse getById(@PathVariable UUID id) {
        return SectionResponse.from(sectionService.getById(id));
    }

    @PutMapping("/sections/{id}")
    public SectionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSectionRequest request) {
        var command = new UpdateSectionCommand(request.title(), request.description(), request.sortOrder());
        return SectionResponse.from(sectionService.update(id, command));
    }

    @DeleteMapping("/sections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        sectionService.delete(id);
    }
}
