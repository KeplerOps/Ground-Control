package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CopyTestCaseCommand;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseCommand;
import com.keplerops.groundcontrol.domain.testcases.service.MoveTestCaseCommand;
import com.keplerops.groundcontrol.domain.testcases.service.ReorderTestCasesCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseCommand;
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
@RequestMapping("/api/v1/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final ProjectService projectService;

    public TestCaseController(TestCaseService testCaseService, ProjectService projectService) {
        this.testCaseService = testCaseService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(
            @Valid @RequestBody TestCaseRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return TestCaseResponse.from(testCaseService.create(new CreateTestCaseCommand(
                projectId,
                request.uid(),
                request.title(),
                request.type(),
                request.priority(),
                request.format(),
                request.description(),
                request.preconditions(),
                request.postconditions(),
                request.estimatedDurationSeconds(),
                request.parentFolderId(),
                request.sortOrder())));
    }

    @GetMapping
    public List<TestCaseResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return testCaseService.listByProject(projectId).stream()
                .map(TestCaseResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TestCaseResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public TestCaseResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public TestCaseResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestCaseRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.update(
                projectId,
                id,
                new UpdateTestCaseCommand(
                        request.title(),
                        request.type(),
                        request.priority(),
                        request.description(),
                        request.preconditions(),
                        request.postconditions(),
                        request.estimatedDurationSeconds(),
                        Boolean.TRUE.equals(request.clearDescription()),
                        Boolean.TRUE.equals(request.clearPreconditions()),
                        Boolean.TRUE.equals(request.clearPostconditions()),
                        Boolean.TRUE.equals(request.clearEstimatedDuration()))));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testCaseService.delete(projectId, id);
    }

    @PutMapping("/{id}/status")
    public TestCaseResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TestCaseStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.transitionStatus(projectId, id, request.status()));
    }

    @PutMapping("/{id}/move")
    public TestCaseResponse move(
            @PathVariable UUID id,
            @Valid @RequestBody MoveTestCaseRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.move(
                projectId, id, new MoveTestCaseCommand(request.parentFolderId(), request.sortOrder())));
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse copy(
            @PathVariable UUID id,
            @Valid @RequestBody CopyTestCaseRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseResponse.from(testCaseService.copy(
                projectId,
                id,
                new CopyTestCaseCommand(request.newUid(), request.parentFolderId(), request.sortOrder())));
    }

    @PutMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(
            @Valid @RequestBody ReorderTestCasesRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testCaseService.reorder(
                projectId, new ReorderTestCasesCommand(request.parentFolderId(), request.orderedTestCaseIds()));
    }
}
