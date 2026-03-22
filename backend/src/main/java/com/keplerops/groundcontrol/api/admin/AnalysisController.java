package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.SimilarityService;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final SimilarityService similarityService;
    private final ProjectService projectService;
    private final double defaultSimilarityThreshold;

    public AnalysisController(
            AnalysisService analysisService,
            SimilarityService similarityService,
            ProjectService projectService,
            @Value("${groundcontrol.embedding.similarity-threshold:0.85}") double defaultSimilarityThreshold) {
        this.analysisService = analysisService;
        this.similarityService = similarityService;
        this.projectService = projectService;
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
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

    @GetMapping("/completeness")
    public CompletenessResponse analyzeCompleteness(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return CompletenessResponse.from(analysisService.analyzeCompleteness(projectId));
    }

    @GetMapping("/cross-wave")
    public List<RelationValidationResponse> crossWaveValidation(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return analysisService.crossWaveValidation(projectId).stream()
                .map(RelationValidationResponse::from)
                .toList();
    }

    @GetMapping("/work-order")
    public WorkOrderResponse getWorkOrder(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return WorkOrderResponse.from(analysisService.getWorkOrder(projectId));
    }

    @GetMapping("/dashboard-stats")
    public DashboardStatsResponse getDashboardStats(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return DashboardStatsResponse.from(analysisService.getDashboardStats(projectId));
    }

    @GetMapping("/semantic-similarity")
    public SimilarityResultResponse findSemanticSimilarity(
            @RequestParam(required = false) String project, @RequestParam(required = false) Double threshold) {
        var projectId = projectService.resolveProjectId(project);
        var effectiveThreshold = threshold != null ? threshold : defaultSimilarityThreshold;
        return SimilarityResultResponse.from(similarityService.findSimilarRequirements(projectId, effectiveThreshold));
    }
}
