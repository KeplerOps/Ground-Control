package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseFolderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TC-005 / ADR-043 — Tree read for hierarchical test repository browsing.
 * Returns a deterministic nested representation of folders and the test
 * cases placed inside them, sorted by container-local {@code sortOrder}.
 */
@RestController
@RequestMapping("/api/v1/test-cases/tree")
public class TestCaseTreeController {

    private final TestCaseFolderService folderService;
    private final ProjectService projectService;

    public TestCaseTreeController(TestCaseFolderService folderService, ProjectService projectService) {
        this.folderService = folderService;
        this.projectService = projectService;
    }

    @GetMapping
    public List<TestCaseTreeNodeResponse> getTree(@RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return folderService.getTree(projectId).stream()
                .map(TestCaseTreeNodeResponse::from)
                .toList();
    }
}
