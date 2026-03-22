package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubIssueSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final RequirementRepository requirementRepository;
    private final TraceabilityService traceabilityService;

    public GitHubIssueSyncService(
            GitHubClient gitHubClient,
            GitHubIssueSyncRepository issueSyncRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            RequirementImportRepository importRepository,
            RequirementRepository requirementRepository,
            TraceabilityService traceabilityService) {
        this.gitHubClient = gitHubClient;
        this.issueSyncRepository = issueSyncRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.importRepository = importRepository;
        this.requirementRepository = requirementRepository;
        this.traceabilityService = traceabilityService;
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
            } catch (RuntimeException e) {
                log.warn("github_issue_sync_failed: issue={} error={}", issue.number(), e.getMessage());
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
            } catch (RuntimeException e) {
                log.warn(
                        "traceability_link_update_failed: artifact={} error={}",
                        link.getArtifactIdentifier(),
                        e.getMessage());
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

    public CreateGitHubIssueResult createIssueFromRequirement(CreateGitHubIssueCommand command) {
        var requirement = requirementRepository
                .findByProjectIdAndUidIgnoreCase(command.projectId(), command.requirementUid())
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + command.requirementUid()));

        String title = requirement.getUid() + ": " + requirement.getTitle();
        String body = formatIssueBody(requirement, command.extraBody());
        if (command.repo() == null || command.repo().isBlank()) {
            throw new GroundControlException("Repository must be specified", "missing_repo");
        }

        GitHubIssueData issue = gitHubClient.createIssue(command.repo(), title, body, command.labels());

        UUID traceabilityLinkId = null;
        String warning = null;
        try {
            var linkCommand = new CreateTraceabilityLinkCommand(
                    ArtifactType.GITHUB_ISSUE, "#" + issue.number(), issue.url(), title, LinkType.IMPLEMENTS);
            var link = traceabilityService.createLink(requirement.getId(), linkCommand);
            traceabilityLinkId = link.getId();
        } catch (RuntimeException e) {
            warning = "Issue created but traceability link failed: " + e.getMessage();
            log.warn(
                    "traceability_link_creation_failed: requirement={} issue={} error={}",
                    command.requirementUid(),
                    issue.number(),
                    e.getMessage());
        }

        return new CreateGitHubIssueResult(issue.url(), issue.number(), traceabilityLinkId, warning);
    }

    private String formatIssueBody(Requirement req, String extraBody) {
        StringBuilder headerParts = new StringBuilder();
        headerParts.append("**").append(req.getUid()).append("**");
        headerParts.append(" | ").append(req.getRequirementType() != null ? req.getRequirementType() : "FUNCTIONAL");
        headerParts.append(" | ").append(req.getPriority() != null ? req.getPriority() : "SHOULD");
        if (req.getWave() != null) {
            headerParts.append(" | Wave ").append(req.getWave());
        }
        headerParts.append(" | ").append(req.getStatus() != null ? req.getStatus() : "DRAFT");

        StringBuilder body = new StringBuilder();
        body.append("> ").append(headerParts).append("\n\n## Statement\n\n").append(req.getStatement());

        if (req.getRationale() != null && !req.getRationale().isBlank()) {
            body.append("\n\n## Rationale\n\n").append(req.getRationale());
        }

        body.append("\n\n---\n*Created from Ground Control requirement ")
                .append(req.getUid())
                .append("*");

        if (extraBody != null && !extraBody.isBlank()) {
            body.append("\n\n").append(extraBody);
        }

        return body.toString();
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
