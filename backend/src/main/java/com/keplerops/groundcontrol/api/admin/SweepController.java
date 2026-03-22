package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis/sweep")
public class SweepController {

    private final AnalysisSweepService analysisSweepService;

    public SweepController(AnalysisSweepService analysisSweepService) {
        this.analysisSweepService = analysisSweepService;
    }

    @PostMapping
    public SweepReportResponse sweep(@RequestParam(required = false) String project) {
        return SweepReportResponse.from(analysisSweepService.sweep(project));
    }

    @PostMapping("/all")
    public List<SweepReportResponse> sweepAll() {
        return analysisSweepService.sweepAll().stream()
                .map(SweepReportResponse::from)
                .toList();
    }
}
