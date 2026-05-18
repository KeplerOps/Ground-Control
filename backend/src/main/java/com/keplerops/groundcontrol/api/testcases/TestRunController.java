package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestRunCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestRunService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestRunCaseResultCommand;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestRunCommand;
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
 * TC-008 / ADR-049 — REST surface for the {@link
 * com.keplerops.groundcontrol.domain.testcases.model.TestRun} aggregate.
 * Routes live under {@code /api/v1/test-runs/**} so existing auth, IP
 * allow-list, and actor-filter chains apply unchanged via the shared
 * {@code /api/v1/**} {@code .authenticated()} rule in
 * {@code ApiPathMatrix}.
 */
@RestController
@RequestMapping("/api/v1/test-runs")
public class TestRunController {

    private final TestRunService testRunService;
    private final ProjectService projectService;

    public TestRunController(TestRunService testRunService, ProjectService projectService) {
        this.testRunService = testRunService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestRunResponse create(
            @Valid @RequestBody TestRunRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return TestRunResponse.from(testRunService.create(new CreateTestRunCommand(
                projectId,
                request.uid(),
                request.name(),
                request.testPlanId(),
                request.testSuiteId(),
                request.environment(),
                request.version(),
                request.build(),
                request.startAt(),
                request.endAt())));
    }

    @GetMapping
    public List<TestRunResponse> list(@RequestParam(required = false) String project) {
        // resolveProjectId (not require) so single-project deployments work
        // without ?project=, matching TestPlanController / TestSuiteController.
        var projectId = projectService.resolveProjectId(project);
        return testRunService.listByProject(projectId).stream()
                .map(TestRunResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TestRunResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunResponse.from(testRunService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public TestRunResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunResponse.from(testRunService.getByUid(projectId, uid));
    }

    @PutMapping("/{id}")
    public TestRunResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestRunRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunResponse.from(testRunService.update(
                projectId,
                id,
                new UpdateTestRunCommand(
                        request.name(),
                        request.environment(),
                        request.version(),
                        request.build(),
                        request.startAt(),
                        request.endAt(),
                        Boolean.TRUE.equals(request.clearEnvironment()),
                        Boolean.TRUE.equals(request.clearVersion()),
                        Boolean.TRUE.equals(request.clearBuild()),
                        Boolean.TRUE.equals(request.clearStartAt()),
                        Boolean.TRUE.equals(request.clearEndAt()))));
    }

    @PutMapping("/{id}/status")
    public TestRunResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TestRunStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunResponse.from(testRunService.transitionStatus(projectId, id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testRunService.delete(projectId, id);
    }

    // ------------------------------------------------------------------
    // Testers
    // ------------------------------------------------------------------

    @PostMapping("/{id}/testers")
    @ResponseStatus(HttpStatus.CREATED)
    public TestRunTesterAssignmentResponse addTester(
            @PathVariable UUID id,
            @Valid @RequestBody AddTestRunTesterRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunTesterAssignmentResponse.from(testRunService.addTester(projectId, id, request.testerName()));
    }

    @GetMapping("/{id}/testers")
    public List<TestRunTesterAssignmentResponse> listTesters(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testRunService.listTesters(projectId, id).stream()
                .map(TestRunTesterAssignmentResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/testers/{testerName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTester(
            @PathVariable UUID id, @PathVariable String testerName, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testRunService.removeTester(projectId, id, testerName);
    }

    // ------------------------------------------------------------------
    // Per-case results
    // ------------------------------------------------------------------

    @GetMapping("/{id}/results")
    public List<TestRunCaseResultResponse> listResults(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testRunService.listResults(projectId, id).stream()
                .map(TestRunCaseResultResponse::from)
                .toList();
    }

    @PutMapping("/{id}/results/{testCaseId}")
    public TestRunCaseResultResponse updateResult(
            @PathVariable UUID id,
            @PathVariable UUID testCaseId,
            @Valid @RequestBody UpdateTestRunCaseResultRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestRunCaseResultResponse.from(testRunService.updateResult(
                projectId,
                id,
                testCaseId,
                new UpdateTestRunCaseResultCommand(
                        request.status(), request.notes(), Boolean.TRUE.equals(request.clearNotes()))));
    }
}
