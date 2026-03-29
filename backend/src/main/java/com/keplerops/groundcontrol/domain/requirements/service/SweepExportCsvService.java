package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SweepExportCsvService {

    public String toCsv(SweepReport report) {
        var sb = new StringBuilder();

        appendSummary(sb, report);
        appendCycles(sb, report.cycles());
        appendOrphans(sb, report.orphans());
        appendCoverageGaps(sb, report.coverageGaps());
        appendCrossWaveViolations(sb, report.crossWaveViolations());
        appendConsistencyViolations(sb, report.consistencyViolations());
        appendCompleteness(sb, report.completeness());
        appendQualityGates(sb, report);

        return sb.toString();
    }

    private void appendSummary(StringBuilder sb, SweepReport report) {
        sb.append("# Sweep Report\n");
        sb.append("project,timestamp,has_problems,total_problems\n");
        sb.append(CsvUtils.escapeCsv(report.projectIdentifier())).append(',');
        sb.append(CsvUtils.escapeCsv(report.timestamp().toString())).append(',');
        sb.append(report.hasProblems()).append(',');
        sb.append(report.totalProblems());
        sb.append('\n');
    }

    private void appendCycles(StringBuilder sb, List<CycleResult> cycles) {
        sb.append("\n# Cycles\n");
        sb.append("cycle_members,edge_source,edge_target,edge_type\n");
        for (var cycle : cycles) {
            String members = String.join(" -> ", cycle.members());
            for (var edge : cycle.edges()) {
                sb.append(CsvUtils.escapeCsv(members)).append(',');
                sb.append(CsvUtils.escapeCsv(edge.sourceUid())).append(',');
                sb.append(CsvUtils.escapeCsv(edge.targetUid())).append(',');
                sb.append(CsvUtils.escapeCsv(
                        edge.relationType() != null ? edge.relationType().name() : ""));
                sb.append('\n');
            }
        }
    }

    private void appendOrphans(StringBuilder sb, List<SweepReport.RequirementSummary> orphans) {
        sb.append("\n# Orphans\n");
        sb.append("uid,title\n");
        for (var orphan : orphans) {
            sb.append(CsvUtils.escapeCsv(orphan.uid())).append(',');
            sb.append(CsvUtils.escapeCsv(orphan.title()));
            sb.append('\n');
        }
    }

    private void appendCoverageGaps(StringBuilder sb, Map<String, List<SweepReport.RequirementSummary>> coverageGaps) {
        sb.append("\n# Coverage Gaps\n");
        sb.append("link_type,uid,title\n");
        for (var entry : coverageGaps.entrySet()) {
            for (var req : entry.getValue()) {
                sb.append(CsvUtils.escapeCsv(entry.getKey())).append(',');
                sb.append(CsvUtils.escapeCsv(req.uid())).append(',');
                sb.append(CsvUtils.escapeCsv(req.title()));
                sb.append('\n');
            }
        }
    }

    private void appendCrossWaveViolations(StringBuilder sb, List<SweepReport.CrossWaveViolationSummary> violations) {
        sb.append("\n# Cross-Wave Violations\n");
        sb.append("source_uid,source_wave,target_uid,target_wave,relation_type\n");
        for (var v : violations) {
            sb.append(CsvUtils.escapeCsv(v.sourceUid())).append(',');
            sb.append(v.sourceWave() != null ? v.sourceWave() : "").append(',');
            sb.append(CsvUtils.escapeCsv(v.targetUid())).append(',');
            sb.append(v.targetWave() != null ? v.targetWave() : "").append(',');
            sb.append(CsvUtils.escapeCsv(v.relationType()));
            sb.append('\n');
        }
    }

    private void appendConsistencyViolations(
            StringBuilder sb, List<SweepReport.ConsistencyViolationSummary> violations) {
        sb.append("\n# Consistency Violations\n");
        sb.append("source_uid,source_status,target_uid,target_status,violation_type\n");
        for (var v : violations) {
            sb.append(CsvUtils.escapeCsv(v.sourceUid())).append(',');
            sb.append(CsvUtils.escapeCsv(v.sourceStatus())).append(',');
            sb.append(CsvUtils.escapeCsv(v.targetUid())).append(',');
            sb.append(CsvUtils.escapeCsv(v.targetStatus())).append(',');
            sb.append(CsvUtils.escapeCsv(v.violationType()));
            sb.append('\n');
        }
    }

    private void appendCompleteness(StringBuilder sb, CompletenessResult completeness) {
        sb.append("\n# Completeness\n");
        sb.append("total,status,count\n");
        for (var entry : completeness.byStatus().entrySet()) {
            sb.append(completeness.total()).append(',');
            sb.append(CsvUtils.escapeCsv(entry.getKey())).append(',');
            sb.append(entry.getValue());
            sb.append('\n');
        }
        if (!completeness.issues().isEmpty()) {
            sb.append("\n# Completeness Issues\n");
            sb.append("uid,issue\n");
            for (var issue : completeness.issues()) {
                sb.append(CsvUtils.escapeCsv(issue.uid())).append(',');
                sb.append(CsvUtils.escapeCsv(issue.issue()));
                sb.append('\n');
            }
        }
    }

    private void appendQualityGates(StringBuilder sb, SweepReport report) {
        if (report.qualityGateResults() == null) {
            return;
        }
        var qg = report.qualityGateResults();
        sb.append("\n# Quality Gates\n");
        sb.append("gate_name,metric_type,metric_param,operator,threshold,actual_value,passed\n");
        for (var gate : qg.gates()) {
            sb.append(CsvUtils.escapeCsv(gate.gateName())).append(',');
            sb.append(CsvUtils.escapeCsv(gate.metricType())).append(',');
            sb.append(CsvUtils.escapeCsv(gate.metricParam())).append(',');
            sb.append(CsvUtils.escapeCsv(gate.operator())).append(',');
            sb.append(gate.threshold()).append(',');
            sb.append(gate.actualValue()).append(',');
            sb.append(gate.passed());
            sb.append('\n');
        }
    }
}
