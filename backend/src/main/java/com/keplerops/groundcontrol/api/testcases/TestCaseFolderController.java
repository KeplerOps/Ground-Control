package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseFolderCommand;
import com.keplerops.groundcontrol.domain.testcases.service.MoveTestCaseFolderCommand;
import com.keplerops.groundcontrol.domain.testcases.service.ReorderTestCaseFoldersCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseFolderService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseFolderCommand;
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

/**
 * TC-005 / ADR-043 — REST surface for the test-case folder aggregate. Paths
 * stay under {@code /api/v1/test-cases/**} so existing auth, IP allow-list,
 * and actor-filter chains apply unchanged.
 */
@RestController
@RequestMapping("/api/v1/test-cases/folders")
public class TestCaseFolderController {

    private final TestCaseFolderService folderService;
    private final ProjectService projectService;

    public TestCaseFolderController(TestCaseFolderService folderService, ProjectService projectService) {
        this.folderService = folderService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseFolderResponse create(
            @Valid @RequestBody TestCaseFolderRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateTestCaseFolderCommand(
                projectId, request.parentFolderId(), request.title(), request.description(), request.sortOrder());
        return TestCaseFolderResponse.from(folderService.create(command));
    }

    @GetMapping
    public List<TestCaseFolderResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return folderService.listByProject(projectId).stream()
                .map(TestCaseFolderResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TestCaseFolderResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseFolderResponse.from(folderService.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public TestCaseFolderResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestCaseFolderRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        var command = new UpdateTestCaseFolderCommand(
                request.title(), request.description(), Boolean.TRUE.equals(request.clearDescription()));
        return TestCaseFolderResponse.from(folderService.update(projectId, id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        folderService.delete(projectId, id);
    }

    @PutMapping("/{id}/move")
    public TestCaseFolderResponse move(
            @PathVariable UUID id,
            @Valid @RequestBody MoveTestCaseFolderRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        var command = new MoveTestCaseFolderCommand(request.parentFolderId(), request.sortOrder());
        return TestCaseFolderResponse.from(folderService.move(projectId, id, command));
    }

    @PutMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(
            @Valid @RequestBody ReorderTestCaseFoldersRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        folderService.reorder(
                projectId, new ReorderTestCaseFoldersCommand(request.parentFolderId(), request.orderedFolderIds()));
    }
}
