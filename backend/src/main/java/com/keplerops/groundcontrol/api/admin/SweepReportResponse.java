package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.api.qualitygates.QualityGateEvaluationResponse;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SweepReportResponse(
        String projectIdentifier,
        Instant timestamp,
        boolean hasProblems,
        int totalProblems,
        List<CycleResponse> cycles,
        List<RequirementRef> orphans,
        Map<String, List<RequirementRef>> coverageGaps,
        List<CrossWaveViolationRef> crossWaveViolations,
        List<ConsistencyViolationRef> consistencyViolations,
        CompletenessResponse completeness,
        QualityGateEvaluationResponse qualityGateResults) {

    public static SweepReportResponse from(SweepReport report) {
        var cycles = report.cycles().stream().map(CycleResponse::from).toList();

        var orphans = report.orphans().stream()
                .map(s -> new RequirementRef(s.uid(), s.title()))
                .toList();

        Map<String, List<RequirementRef>> coverageGaps = new LinkedHashMap<>();
        for (var entry : report.coverageGaps().entrySet()) {
            coverageGaps.put(
                    entry.getKey(),
                    entry.getValue().stream()
                            .map(s -> new RequirementRef(s.uid(), s.title()))
                            .toList());
        }

        var crossWave = report.crossWaveViolations().stream()
                .map(v -> new CrossWaveViolationRef(
                        v.sourceUid(), v.sourceWave(), v.targetUid(), v.targetWave(), v.relationType()))
                .toList();

        var consistency = report.consistencyViolations().stream()
                .map(v -> new ConsistencyViolationRef(
                        v.sourceUid(), v.sourceStatus(), v.targetUid(), v.targetStatus(), v.violationType()))
                .toList();

        var qualityGateResults = report.qualityGateResults() != null
                ? QualityGateEvaluationResponse.from(report.qualityGateResults())
                : null;

        return new SweepReportResponse(
                report.projectIdentifier(),
                report.timestamp(),
                report.hasProblems(),
                report.totalProblems(),
                cycles,
                orphans,
                coverageGaps,
                crossWave,
                consistency,
                CompletenessResponse.from(report.completeness()),
                qualityGateResults);
    }

    public record RequirementRef(String uid, String title) {}

    public record CrossWaveViolationRef(
            String sourceUid, Integer sourceWave, String targetUid, Integer targetWave, String relationType) {}

    public record ConsistencyViolationRef(
            String sourceUid, String sourceStatus, String targetUid, String targetStatus, String violationType) {}
}
