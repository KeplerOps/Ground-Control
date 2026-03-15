package com.keplerops.groundcontrol.api.admin;

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

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/cycles")
    public List<CycleResponse> detectCycles() {
        return analysisService.detectCycles().stream().map(CycleResponse::from).toList();
    }

    @GetMapping("/orphans")
    public List<RequirementSummaryResponse> findOrphans() {
        return analysisService.findOrphans().stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/coverage-gaps")
    public List<RequirementSummaryResponse> findCoverageGaps(@RequestParam LinkType linkType) {
        return analysisService.findCoverageGaps(linkType).stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/impact/{id}")
    public List<RequirementSummaryResponse> impactAnalysis(@PathVariable UUID id) {
        return analysisService.impactAnalysis(id).stream()
                .map(RequirementSummaryResponse::from)
                .toList();
    }

    @GetMapping("/cross-wave")
    public List<RelationValidationResponse> crossWaveValidation() {
        return analysisService.crossWaveValidation().stream()
                .map(RelationValidationResponse::from)
                .toList();
    }
}
