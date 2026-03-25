package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SweepReport(
        String projectIdentifier,
        Instant timestamp,
        List<CycleResult> cycles,
        List<RequirementSummary> orphans,
        Map<String, List<RequirementSummary>> coverageGaps,
        List<CrossWaveViolationSummary> crossWaveViolations,
        List<ConsistencyViolationSummary> consistencyViolations,
        CompletenessResult completeness,
        QualityGateEvaluationResult qualityGateResults) {

    public boolean hasProblems() {
        return !cycles.isEmpty()
                || !orphans.isEmpty()
                || coverageGaps.values().stream().anyMatch(l -> !l.isEmpty())
                || !crossWaveViolations.isEmpty()
                || !consistencyViolations.isEmpty()
                || (qualityGateResults != null && !qualityGateResults.passed());
    }

    /** Counts each distinct finding; a requirement may appear in multiple categories. */
    public int totalProblems() {
        int count = cycles.size() + orphans.size() + crossWaveViolations.size() + consistencyViolations.size();
        for (List<RequirementSummary> gaps : coverageGaps.values()) {
            count += gaps.size();
        }
        if (qualityGateResults != null) {
            count += qualityGateResults.failedCount();
        }
        return count;
    }

    public record RequirementSummary(String uid, String title) {}

    public record CrossWaveViolationSummary(
            String sourceUid, Integer sourceWave, String targetUid, Integer targetWave, String relationType) {}

    public record ConsistencyViolationSummary(
            String sourceUid, String sourceStatus, String targetUid, String targetStatus, String violationType) {}
}
