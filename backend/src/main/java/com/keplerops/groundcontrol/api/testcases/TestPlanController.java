package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestPlanCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestPlanService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestPlanCommand;
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
 * TC-006 / ADR-044 — REST surface for the {@link
 * com.keplerops.groundcontrol.domain.testcases.model.TestPlan} aggregate.
 * Routes live under {@code /api/v1/test-plans/**} so existing auth, IP
 * allow-list, and actor-filter chains apply unchanged.
 */
@RestController
@RequestMapping("/api/v1/test-plans")
public class TestPlanController {

    private final TestPlanService testPlanService;
    private final ProjectService projectService;

    public TestPlanController(TestPlanService testPlanService, ProjectService projectService) {
        this.testPlanService = testPlanService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestPlanResponse create(
            @Valid @RequestBody TestPlanRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return TestPlanResponse.from(testPlanService.create(new CreateTestPlanCommand(
                projectId,
                request.uid(),
                request.name(),
                request.description(),
                request.product(),
                request.version(),
                request.build(),
                request.startDate(),
                request.endDate())));
    }

    @GetMapping
    public List<TestPlanResponse> list(@RequestParam(required = false) String project) {
        // resolveProjectId (not requireProjectId) so single-project deployments
        // can list without an explicit ?project= param, matching TestCaseController
        // and TreatmentPlanController list endpoints (codex pre-push cycle 1).
        var projectId = projectService.resolveProjectId(project);
        return testPlanService.listByProject(projectId).stream()
                .map(TestPlanResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TestPlanResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestPlanResponse.from(testPlanService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public TestPlanResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestPlanResponse.from(testPlanService.getByUid(projectId, uid));
    }

    @PutMapping("/{id}")
    public TestPlanResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestPlanRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestPlanResponse.from(testPlanService.update(
                projectId,
                id,
                new UpdateTestPlanCommand(
                        request.name(),
                        request.description(),
                        request.product(),
                        request.version(),
                        request.build(),
                        request.startDate(),
                        request.endDate(),
                        Boolean.TRUE.equals(request.clearDescription()),
                        Boolean.TRUE.equals(request.clearProduct()),
                        Boolean.TRUE.equals(request.clearVersion()),
                        Boolean.TRUE.equals(request.clearBuild()),
                        Boolean.TRUE.equals(request.clearStartDate()),
                        Boolean.TRUE.equals(request.clearEndDate()))));
    }

    @PutMapping("/{id}/status")
    public TestPlanResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TestPlanStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestPlanResponse.from(testPlanService.transitionStatus(projectId, id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testPlanService.delete(projectId, id);
    }
}
