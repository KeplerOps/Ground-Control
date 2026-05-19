package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.AddTestSuiteMemberCommand;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestSuiteCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestSuiteCriteriaCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestSuiteService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestSuiteCommand;
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
 * TC-007 / ADR-047 — REST surface for the {@link
 * com.keplerops.groundcontrol.domain.testcases.model.TestSuite}
 * aggregate. Routes live under {@code /api/v1/test-suites/**} so existing
 * auth, IP allow-list, and actor-filter chains apply unchanged via
 * {@code ApiPathMatrix.applySharedRules}'s {@code /api/v1/**}
 * {@code .authenticated()} rule.
 */
@RestController
@RequestMapping("/api/v1/test-suites")
public class TestSuiteController {

    private final TestSuiteService testSuiteService;
    private final ProjectService projectService;

    public TestSuiteController(TestSuiteService testSuiteService, ProjectService projectService) {
        this.testSuiteService = testSuiteService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestSuiteResponse create(
            @Valid @RequestBody TestSuiteRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var criteria = new TestSuiteCriteriaCommand(
                request.criteriaStatus(),
                request.criteriaType(),
                request.criteriaPriority(),
                request.criteriaFormat(),
                request.criteriaFolderId(),
                request.criteriaTextSearch());
        return TestSuiteResponse.from(testSuiteService.create(new CreateTestSuiteCommand(
                projectId, request.uid(), request.name(), request.description(), request.populationMode(), criteria)));
    }

    @GetMapping
    public List<TestSuiteResponse> list(@RequestParam(required = false) String project) {
        // resolveProjectId (not requireProjectId) so single-project deployments
        // can list without an explicit ?project= param, matching the rest of
        // the test-management list endpoints.
        var projectId = projectService.resolveProjectId(project);
        return testSuiteService.listByProject(projectId).stream()
                .map(TestSuiteResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TestSuiteResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestSuiteResponse.from(testSuiteService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public TestSuiteResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestSuiteResponse.from(testSuiteService.getByUid(projectId, uid));
    }

    @PutMapping("/{id}")
    public TestSuiteResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestSuiteRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestSuiteResponse.from(testSuiteService.update(
                projectId,
                id,
                new UpdateTestSuiteCommand(
                        request.name(),
                        request.description(),
                        request.criteriaStatus(),
                        request.criteriaType(),
                        request.criteriaPriority(),
                        request.criteriaFormat(),
                        request.criteriaFolderId(),
                        request.criteriaTextSearch(),
                        Boolean.TRUE.equals(request.clearDescription()),
                        Boolean.TRUE.equals(request.clearCriteriaStatus()),
                        Boolean.TRUE.equals(request.clearCriteriaType()),
                        Boolean.TRUE.equals(request.clearCriteriaPriority()),
                        Boolean.TRUE.equals(request.clearCriteriaFormat()),
                        Boolean.TRUE.equals(request.clearCriteriaFolderId()),
                        Boolean.TRUE.equals(request.clearCriteriaTextSearch()))));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testSuiteService.delete(projectId, id);
    }

    @GetMapping("/{id}/test-cases")
    public List<TestCaseResponse> resolveTestCases(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testSuiteService.resolveTestCases(projectId, id).stream()
                .map(TestCaseResponse::from)
                .toList();
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TestSuiteMemberResponse addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddTestSuiteMemberRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        var member = testSuiteService.addMember(
                projectId, id, new AddTestSuiteMemberCommand(request.testCaseId(), request.position()));
        return TestSuiteMemberResponse.from(member);
    }

    @DeleteMapping("/{id}/members/{testCaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID id, @PathVariable UUID testCaseId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testSuiteService.removeMember(projectId, id, testCaseId);
    }

    @PutMapping("/{id}/members/reorder")
    public List<TestSuiteMemberResponse> reorderMembers(
            @PathVariable UUID id,
            @Valid @RequestBody ReorderTestSuiteMembersRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testSuiteService.reorderMembers(projectId, id, request.orderedTestCaseIds()).stream()
                .map(TestSuiteMemberResponse::from)
                .toList();
    }

    @GetMapping("/{id}/members")
    public List<TestSuiteMemberResponse> listMembers(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testSuiteService.listMembers(projectId, id).stream()
                .map(TestSuiteMemberResponse::from)
                .toList();
    }

    @PostMapping("/{id}/source-requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public TestSuiteSourceRequirementResponse addSourceRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody AddTestSuiteSourceRequirementRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return TestSuiteSourceRequirementResponse.from(
                testSuiteService.addSourceRequirement(projectId, id, request.requirementId()));
    }

    @DeleteMapping("/{id}/source-requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSourceRequirement(
            @PathVariable UUID id, @PathVariable UUID requirementId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        testSuiteService.removeSourceRequirement(projectId, id, requirementId);
    }

    @GetMapping("/{id}/source-requirements")
    public List<TestSuiteSourceRequirementResponse> listSourceRequirements(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return testSuiteService.listSourceRequirements(projectId, id).stream()
                .map(TestSuiteSourceRequirementResponse::from)
                .toList();
    }
}
