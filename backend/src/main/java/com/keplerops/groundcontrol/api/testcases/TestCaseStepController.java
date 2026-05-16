package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseStepService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseStepCommand;
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
@RequestMapping("/api/v1/test-cases/{testCaseId}/steps")
public class TestCaseStepController {

    private final TestCaseStepService stepService;
    private final ProjectService projectService;

    public TestCaseStepController(TestCaseStepService stepService, ProjectService projectService) {
        this.stepService = stepService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseStepResponse create(
            @PathVariable UUID testCaseId,
            @Valid @RequestBody TestCaseStepRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return TestCaseStepResponse.from(stepService.create(new CreateTestCaseStepCommand(
                projectId,
                testCaseId,
                request.stepNumber(),
                request.action(),
                request.expectedResult(),
                request.actualResult())));
    }

    @GetMapping
    public List<TestCaseStepResponse> list(
            @PathVariable UUID testCaseId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return stepService.listByTestCase(projectId, testCaseId).stream()
                .map(TestCaseStepResponse::from)
                .toList();
    }

    @GetMapping("/{stepId}")
    public TestCaseStepResponse getById(
            @PathVariable UUID testCaseId, @PathVariable UUID stepId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseStepResponse.from(stepService.getById(projectId, testCaseId, stepId));
    }

    @PutMapping("/{stepId}")
    public TestCaseStepResponse update(
            @PathVariable UUID testCaseId,
            @PathVariable UUID stepId,
            @Valid @RequestBody UpdateTestCaseStepRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseStepResponse.from(stepService.update(
                projectId,
                testCaseId,
                stepId,
                new UpdateTestCaseStepCommand(
                        request.stepNumber(),
                        request.action(),
                        request.expectedResult(),
                        request.actualResult(),
                        Boolean.TRUE.equals(request.clearActualResult()))));
    }

    @DeleteMapping("/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID testCaseId, @PathVariable UUID stepId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        stepService.delete(projectId, testCaseId, stepId);
    }
}
