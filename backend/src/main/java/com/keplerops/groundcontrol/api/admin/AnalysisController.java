package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final ProjectService projectService;

    public AnalysisController(AnalysisService analysisService, ProjectService projectService) {
        this.analysisService = analysisService;
        this.projectService = projectService;
    }

    @GetMapping("/cycles")
    public List<CycleResponse> detectCycles(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.detectCycles(projectId).stream()
                .map(CycleResponse::from)
                .toList();
    }

    @GetMapping("/orphans")
    public List<RequirementSummaryResponse> findOrphans(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.findOrphans(projectId).stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/coverage-gaps")
    public List<RequirementSummaryResponse> findCoverageGaps(
            @RequestParam LinkType linkType, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.findCoverageGaps(projectId, linkType).stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/impact/{id}")
    public List<RequirementSummaryResponse> impactAnalysis(@PathVariable UUID id) {
        return analysisService.impactAnalysis(id).stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/consistency-violations")
    public List<ConsistencyViolationResponse> detectConsistencyViolations(
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.detectConsistencyViolations(projectId).stream()
                .map(ConsistencyViolationResponse::from)
                .toList();
    }

    @GetMapping("/cross-wave")
    public List<RelationValidationResponse> crossWaveValidation(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.crossWaveValidation(projectId).stream()
                .map(RelationValidationResponse::from)
                .toList();
    }
}
