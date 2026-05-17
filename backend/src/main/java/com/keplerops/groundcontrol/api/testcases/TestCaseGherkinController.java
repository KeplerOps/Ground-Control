package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseGherkinCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseGherkinService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseGherkinCommand;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/test-cases/{testCaseId}/gherkin")
public class TestCaseGherkinController {

    private final TestCaseGherkinService gherkinService;
    private final ProjectService projectService;

    public TestCaseGherkinController(TestCaseGherkinService gherkinService, ProjectService projectService) {
        this.gherkinService = gherkinService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseGherkinResponse create(
            @PathVariable UUID testCaseId,
            @Valid @RequestBody TestCaseGherkinRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return TestCaseGherkinResponse.from(
                gherkinService.create(new CreateTestCaseGherkinCommand(projectId, testCaseId, request.source())));
    }

    @GetMapping
    public TestCaseGherkinResponse getByTestCase(
            @PathVariable UUID testCaseId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseGherkinResponse.from(gherkinService.getByTestCase(projectId, testCaseId));
    }

    @PutMapping
    public TestCaseGherkinResponse update(
            @PathVariable UUID testCaseId,
            @Valid @RequestBody UpdateTestCaseGherkinRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestCaseGherkinResponse.from(
                gherkinService.update(projectId, testCaseId, new UpdateTestCaseGherkinCommand(request.source())));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID testCaseId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        gherkinService.deleteByTestCase(projectId, testCaseId);
    }
}
