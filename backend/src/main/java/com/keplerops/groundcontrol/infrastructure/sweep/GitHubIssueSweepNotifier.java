package com.keplerops.groundcontrol.infrastructure.sweep;

import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubClient;
import com.keplerops.groundcontrol.domain.requirements.service.SweepNotifier;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport.ConsistencyViolationSummary;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport.CrossWaveViolationSummary;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport.RequirementSummary;
import jakarta.annotation.PostConstruct;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"groundcontrol.sweep.enabled", "groundcontrol.sweep.github.enabled"},
        havingValue = "true")
public class GitHubIssueSweepNotifier implements SweepNotifier {

    private static final Logger log = LoggerFactory.getLogger(GitHubIssueSweepNotifier.class);
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final GitHubClient gitHubClient;
    private final SweepProperties properties;

    public GitHubIssueSweepNotifier(GitHubClient gitHubClient, SweepProperties properties) {
        this.gitHubClient = gitHubClient;
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (properties.github().repo() == null || properties.github().repo().isBlank()) {
            throw new IllegalStateException(
                    "groundcontrol.sweep.github.repo must be set when GitHub notifications are enabled");
        }
    }

    @Override
    public void notify(SweepReport report) {
        // Each sweep creates a new issue; deduplication is not yet implemented.
        // TODO: search for open sweep issues before creating a new one to avoid duplicates.
        var title =
                String.format("[Sweep] %d problems detected in %s", report.totalProblems(), report.projectIdentifier());
        var body = formatBody(report);
        var repo = properties.github().repo();
        var labels = properties.github().labels();

        var issue = gitHubClient.createIssue(repo, title, body, labels);
        log.info("sweep_github_issue_created: project={} issue={}", report.projectIdentifier(), issue.number());
    }

    static String formatBody(SweepReport report) {
        var sb = new StringBuilder();
        sb.append("## Analysis Sweep Report\n\n");
        sb.append("**Project:** ").append(report.projectIdentifier()).append("\n");
        sb.append("**Timestamp:** ")
                .append(TIMESTAMP_FMT.format(report.timestamp()))
                .append(" UTC\n");
        sb.append("**Total problems:** ").append(report.totalProblems()).append("\n\n");

        if (!report.cycles().isEmpty()) {
            sb.append("### Dependency Cycles\n\n");
            for (CycleResult cycle : report.cycles()) {
                sb.append("- ").append(String.join(" -> ", cycle.members())).append("\n");
            }
            sb.append("\n");
        }

        if (!report.orphans().isEmpty()) {
            sb.append("### Orphan Requirements\n\n");
            appendRequirementList(sb, report.orphans());
            sb.append("\n");
        }

        if (!report.coverageGaps().isEmpty()) {
            sb.append("### Coverage Gaps\n\n");
            for (Map.Entry<String, List<RequirementSummary>> entry :
                    report.coverageGaps().entrySet()) {
                sb.append("**").append(entry.getKey()).append(":**\n");
                appendRequirementList(sb, entry.getValue());
            }
            sb.append("\n");
        }

        if (!report.crossWaveViolations().isEmpty()) {
            sb.append("### Cross-Wave Violations\n\n");
            for (CrossWaveViolationSummary v : report.crossWaveViolations()) {
                sb.append("- ")
                        .append(v.sourceUid())
                        .append(" (wave ")
                        .append(v.sourceWave())
                        .append(") -> ")
                        .append(v.targetUid())
                        .append(" (wave ")
                        .append(v.targetWave())
                        .append(") via ")
                        .append(v.relationType())
                        .append("\n");
            }
            sb.append("\n");
        }

        if (!report.consistencyViolations().isEmpty()) {
            sb.append("### Consistency Violations\n\n");
            for (ConsistencyViolationSummary v : report.consistencyViolations()) {
                sb.append("- ")
                        .append(v.sourceUid())
                        .append(" [")
                        .append(v.sourceStatus())
                        .append("] <-> ")
                        .append(v.targetUid())
                        .append(" [")
                        .append(v.targetStatus())
                        .append("]: ")
                        .append(v.violationType())
                        .append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n*Generated by Ground Control analysis sweep*");
        return sb.toString();
    }

    private static void appendRequirementList(StringBuilder sb, List<RequirementSummary> reqs) {
        for (RequirementSummary req : reqs) {
            sb.append("- ").append(req.uid()).append(": ").append(req.title()).append("\n");
        }
    }
}
