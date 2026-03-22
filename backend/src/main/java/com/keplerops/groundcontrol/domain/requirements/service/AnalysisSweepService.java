package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalysisSweepService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisSweepService.class);

    private final AnalysisService analysisService;
    private final ProjectService projectService;
    private final List<SweepNotifier> notifiers;

    public AnalysisSweepService(
            AnalysisService analysisService, ProjectService projectService, List<SweepNotifier> notifiers) {
        this.analysisService = analysisService;
        this.projectService = projectService;
        this.notifiers = notifiers;
    }

    public SweepReport sweep(String projectIdentifier) {
        var projectId = projectService.resolveProjectId(projectIdentifier);
        var project = projectService.getById(projectId);
        var identifier = project.getIdentifier();

        log.info("sweep_started: project={}", identifier);

        var cycles = analysisService.detectCycles(projectId);

        var orphans = analysisService.findOrphans(projectId).stream()
                .map(AnalysisSweepService::toSummary)
                .toList();

        Map<String, List<SweepReport.RequirementSummary>> coverageGaps = new LinkedHashMap<>();
        for (LinkType linkType : LinkType.values()) {
            var gaps = analysisService.findCoverageGaps(projectId, linkType).stream()
                    .map(AnalysisSweepService::toSummary)
                    .toList();
            if (!gaps.isEmpty()) {
                coverageGaps.put(linkType.name(), gaps);
            }
        }

        var crossWaveViolations = analysisService.crossWaveValidation(projectId).stream()
                .map(AnalysisSweepService::toCrossWaveSummary)
                .toList();

        var consistencyViolations = analysisService.detectConsistencyViolations(projectId).stream()
                .map(AnalysisSweepService::toConsistencySummary)
                .toList();

        var completeness = analysisService.analyzeCompleteness(projectId);

        var report = new SweepReport(
                identifier,
                Instant.now(),
                cycles,
                orphans,
                coverageGaps,
                crossWaveViolations,
                consistencyViolations,
                completeness);

        log.info(
                "sweep_completed: project={} problems={} total={}",
                identifier,
                report.hasProblems(),
                report.totalProblems());

        if (report.hasProblems()) {
            for (SweepNotifier notifier : notifiers) {
                try {
                    notifier.notify(report);
                } catch (RuntimeException e) {
                    log.warn(
                            "sweep_notification_failed: project={} notifier={} error={}",
                            identifier,
                            notifier.getClass().getSimpleName(),
                            e.getMessage());
                }
            }
        }

        return report;
    }

    public List<SweepReport> sweepAll() {
        var projects = projectService.list();
        List<SweepReport> reports = new ArrayList<>();
        for (Project project : projects) {
            try {
                reports.add(sweep(project.getIdentifier()));
            } catch (RuntimeException e) {
                log.error("sweep_project_failed: project={} error={}", project.getIdentifier(), e.getMessage(), e);
            }
        }
        return reports;
    }

    private static SweepReport.RequirementSummary toSummary(Requirement req) {
        return new SweepReport.RequirementSummary(req.getUid(), req.getTitle());
    }

    private static SweepReport.CrossWaveViolationSummary toCrossWaveSummary(RequirementRelation rel) {
        return new SweepReport.CrossWaveViolationSummary(
                rel.getSource().getUid(),
                rel.getSource().getWave(),
                rel.getTarget().getUid(),
                rel.getTarget().getWave(),
                rel.getRelationType().name());
    }

    private static SweepReport.ConsistencyViolationSummary toConsistencySummary(ConsistencyViolation violation) {
        var rel = violation.relation();
        return new SweepReport.ConsistencyViolationSummary(
                rel.getSource().getUid(),
                rel.getSource().getStatus().name(),
                rel.getTarget().getUid(),
                rel.getTarget().getStatus().name(),
                violation.violationType());
    }
}
