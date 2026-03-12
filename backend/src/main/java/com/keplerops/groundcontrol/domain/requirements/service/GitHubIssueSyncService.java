package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubIssueSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GitHubIssueSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitHubIssueSyncService.class);

    private static final Pattern PHASE_RE = Pattern.compile("phase-(\\d+)");
    private static final Pattern PRIORITY_RE = Pattern.compile("^P(\\d)$");
    private static final Pattern ISSUE_REF_RE = Pattern.compile("(?<!\\w)#(\\d+)\\b");

    private final GitHubClient gitHubClient;
    private final GitHubIssueSyncRepository issueSyncRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final RequirementImportRepository importRepository;

    public GitHubIssueSyncService(
            GitHubClient gitHubClient,
            GitHubIssueSyncRepository issueSyncRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            RequirementImportRepository importRepository) {
        this.gitHubClient = gitHubClient;
        this.issueSyncRepository = issueSyncRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.importRepository = importRepository;
    }

    public SyncResult syncGitHubIssues(String owner, String repo) {
        List<GitHubIssueData> fetched = gitHubClient.fetchAllIssues(owner, repo);
        Instant fetchedAt = Instant.now();
        List<Map<String, Object>> errors = new ArrayList<>();

        int issuesCreated = 0;
        int issuesUpdated = 0;

        // Phase 1: Upsert GitHubIssueSync records
        for (GitHubIssueData issue : fetched) {
            try {
                var existing = issueSyncRepository.findByIssueNumber(issue.number());
                GitHubIssueSync sync;
                if (existing.isPresent()) {
                    sync = existing.get();
                    sync.setIssueTitle(issue.title());
                    sync.setIssueState(IssueState.valueOf(issue.state()));
                    sync.setIssueBody(issue.body() != null ? issue.body() : "");
                    sync.setIssueLabels(issue.labels());
                    sync.setPhase(extractPhase(issue.labels()));
                    sync.setPriorityLabel(extractPriority(issue.labels()));
                    sync.setCrossReferences(extractCrossReferences(issue.body(), issue.number()));
                    sync.setLastFetchedAt(fetchedAt);
                    issueSyncRepository.save(sync);
                    issuesUpdated++;
                } else {
                    sync = new GitHubIssueSync(
                            issue.number(), issue.title(), IssueState.valueOf(issue.state()), issue.url(), fetchedAt);
                    sync.setIssueBody(issue.body() != null ? issue.body() : "");
                    sync.setIssueLabels(issue.labels());
                    sync.setPhase(extractPhase(issue.labels()));
                    sync.setPriorityLabel(extractPriority(issue.labels()));
                    sync.setCrossReferences(extractCrossReferences(issue.body(), issue.number()));
                    issueSyncRepository.save(sync);
                    issuesCreated++;
                }
            } catch (Exception e) {
                log.warn("Error syncing issue #{}: {}", issue.number(), e.getMessage());
                errors.add(Map.of("phase", "upsert", "issue", issue.number(), "error", e.getMessage()));
            }
        }

        // Phase 2: Update TraceabilityLinks
        int linksUpdated = 0;
        var links = traceabilityLinkRepository.findByArtifactType(ArtifactType.GITHUB_ISSUE);
        for (var link : links) {
            try {
                int issueNumber = Integer.parseInt(link.getArtifactIdentifier());
                var syncOpt = issueSyncRepository.findByIssueNumber(issueNumber);
                if (syncOpt.isPresent()) {
                    var sync = syncOpt.get();
                    link.setArtifactUrl(sync.getIssueUrl());
                    link.setArtifactTitle(sync.getIssueTitle());
                    link.setSyncStatus(SyncStatus.SYNCED);
                    link.setLastSyncedAt(fetchedAt);
                    traceabilityLinkRepository.save(link);
                    linksUpdated++;
                }
            } catch (Exception e) {
                log.warn("Error updating traceability link {}: {}", link.getArtifactIdentifier(), e.getMessage());
                errors.add(Map.of(
                        "phase",
                        "traceability",
                        "artifactIdentifier",
                        link.getArtifactIdentifier(),
                        "error",
                        e.getMessage()));
            }
        }

        // Save audit record
        var audit = new RequirementImport(ImportSourceType.GITHUB);
        audit.setSourceFile(owner + "/" + repo);
        audit.setStats(Map.of(
                "issuesFetched", fetched.size(),
                "issuesCreated", issuesCreated,
                "issuesUpdated", issuesUpdated,
                "linksUpdated", linksUpdated));
        audit.setErrors(errors);
        var savedAudit = importRepository.save(audit);

        return new SyncResult(
                savedAudit.getId(),
                savedAudit.getImportedAt(),
                fetched.size(),
                issuesCreated,
                issuesUpdated,
                linksUpdated,
                errors);
    }

    Integer extractPhase(List<String> labels) {
        for (String label : labels) {
            Matcher m = PHASE_RE.matcher(label);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return null;
    }

    String extractPriority(List<String> labels) {
        for (String label : labels) {
            Matcher m = PRIORITY_RE.matcher(label);
            if (m.matches()) {
                return label;
            }
        }
        return "";
    }

    List<Integer> extractCrossReferences(String body, int ownNumber) {
        if (body == null || body.isEmpty()) {
            return List.of();
        }
        List<Integer> refs = new ArrayList<>();
        Matcher m = ISSUE_REF_RE.matcher(body);
        while (m.find()) {
            int ref = Integer.parseInt(m.group(1));
            if (ref != ownNumber) {
                refs.add(ref);
            }
        }
        return refs;
    }
}
