package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.GitHubIssueSync;
import com.keplerops.groundcontrol.domain.requirements.model.GitHubPullRequestSync;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementImport;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubIssueSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.GitHubPullRequestSyncRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementImportRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.ImportSourceType;
import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.PullRequestState;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final GitHubPullRequestSyncRepository prSyncRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;
    private final RequirementImportRepository importRepository;
    private final RequirementRepository requirementRepository;
    private final TraceabilityService traceabilityService;

    public GitHubIssueSyncService(
            GitHubClient gitHubClient,
            GitHubIssueSyncRepository issueSyncRepository,
            GitHubPullRequestSyncRepository prSyncRepository,
            TraceabilityLinkRepository traceabilityLinkRepository,
            RequirementImportRepository importRepository,
            RequirementRepository requirementRepository,
            TraceabilityService traceabilityService) {
        this.gitHubClient = gitHubClient;
        this.issueSyncRepository = issueSyncRepository;
        this.prSyncRepository = prSyncRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
        this.importRepository = importRepository;
        this.requirementRepository = requirementRepository;
        this.traceabilityService = traceabilityService;
    }

    public SyncResult syncGitHubIssues(String owner, String repo) {
        List<GitHubIssueData> fetched = gitHubClient.fetchAllIssues(owner, repo);
        Instant fetchedAt = Instant.now();
        List<SyncError> errors = new ArrayList<>();

        int[] issueCounts = upsertIssueSyncRecords(fetched, fetchedAt, errors);
        int issuesCreated = issueCounts[0];
        int issuesUpdated = issueCounts[1];

        int linksUpdated = updateTraceabilityLinks(fetchedAt, errors);

        var audit = new RequirementImport(ImportSourceType.GITHUB);
        audit.setSourceFile(owner + "/" + repo);
        audit.setStats(Map.of(
                "issuesFetched", fetched.size(),
                "issuesCreated", issuesCreated,
                "issuesUpdated", issuesUpdated,
                "linksUpdated", linksUpdated));
        audit.setErrors(toAuditErrors(errors));
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

    private int[] upsertIssueSyncRecords(List<GitHubIssueData> fetched, Instant fetchedAt, List<SyncError> errors) {
        int issuesCreated = 0;
        int issuesUpdated = 0;

        for (GitHubIssueData issue : fetched) {
            try {
                var existing = issueSyncRepository.findByIssueNumber(issue.number());
                if (existing.isPresent()) {
                    updateExistingSync(existing.get(), issue, fetchedAt);
                    issuesUpdated++;
                } else {
                    createNewSync(issue, fetchedAt);
                    issuesCreated++;
                }
            } catch (RuntimeException e) {
                log.warn("github_issue_sync_failed: issue={} error={}", issue.number(), e.getMessage());
                errors.add(new SyncError("upsert", issue.number(), null, e.getMessage()));
            }
        }
        return new int[] {issuesCreated, issuesUpdated};
    }

    private void updateExistingSync(GitHubIssueSync sync, GitHubIssueData issue, Instant fetchedAt) {
        sync.setIssueTitle(issue.title());
        sync.setIssueState(parseIssueState(issue.state()));
        sync.setIssueBody(issue.body() != null ? issue.body() : "");
        sync.setIssueLabels(issue.labels());
        sync.setPhase(extractPhase(issue.labels()));
        sync.setPriorityLabel(extractPriority(issue.labels()));
        sync.setCrossReferences(extractCrossReferences(issue.body(), issue.number()));
        sync.setLastFetchedAt(fetchedAt);
        issueSyncRepository.save(sync);
    }

    private void createNewSync(GitHubIssueData issue, Instant fetchedAt) {
        var sync = new GitHubIssueSync(
                issue.number(), issue.title(), parseIssueState(issue.state()), issue.url(), fetchedAt);
        sync.setIssueBody(issue.body() != null ? issue.body() : "");
        sync.setIssueLabels(issue.labels());
        sync.setPhase(extractPhase(issue.labels()));
        sync.setPriorityLabel(extractPriority(issue.labels()));
        sync.setCrossReferences(extractCrossReferences(issue.body(), issue.number()));
        issueSyncRepository.save(sync);
    }

    private int updateTraceabilityLinks(Instant fetchedAt, List<SyncError> errors) {
        int linksUpdated = 0;
        var links = traceabilityLinkRepository.findByArtifactType(ArtifactType.GITHUB_ISSUE);
        for (var link : links) {
            try {
                int issueNumber = Integer.parseInt(link.getArtifactIdentifier());
                var syncOpt = issueSyncRepository.findByIssueNumber(issueNumber);
                if (syncOpt.isPresent()) {
                    var sync = syncOpt.get();
                    link.setArtifactUrl(sync.getIssueUrl());
                    String stateTag = sync.getIssueState() != null ? " [" + sync.getIssueState() + "]" : "";
                    link.setArtifactTitle("#" + sync.getIssueNumber() + " - " + sync.getIssueTitle() + stateTag);
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
                errors.add(new SyncError("traceability", null, link.getArtifactIdentifier(), e.getMessage()));
            }
        }
        return linksUpdated;
    }

    private static List<Map<String, Object>> toAuditErrors(List<SyncError> errors) {
        return errors.stream()
                .map(e -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("phase", e.phase());
                    if (e.issue() != null) m.put("issue", e.issue());
                    if (e.artifactIdentifier() != null) m.put("artifactIdentifier", e.artifactIdentifier());
                    m.put("error", e.error());
                    return (Map<String, Object>) m;
                })
                .toList();
    }

    // -----------------------------------------------------------------------
    // Pull request sync
    // -----------------------------------------------------------------------

    public PrSyncResult syncGitHubPullRequests(String owner, String repo) {
        List<GitHubPullRequestData> fetched = gitHubClient.fetchAllPullRequests(owner, repo);
        Instant fetchedAt = Instant.now();
        List<SyncError> errors = new ArrayList<>();

        String repoSlug = owner + "/" + repo;
        int[] prCounts = upsertPrSyncRecords(repoSlug, fetched, fetchedAt, errors);
        int prsCreated = prCounts[0];
        int prsUpdated = prCounts[1];

        int linksUpdated = updatePrTraceabilityLinks(repoSlug, fetchedAt, errors);

        var audit = new RequirementImport(ImportSourceType.GITHUB);
        audit.setSourceFile(owner + "/" + repo + " (pull requests)");
        audit.setStats(Map.of(
                "prsFetched", fetched.size(),
                "prsCreated", prsCreated,
                "prsUpdated", prsUpdated,
                "linksUpdated", linksUpdated));
        audit.setErrors(toAuditErrors(errors));
        var savedAudit = importRepository.save(audit);

        return new PrSyncResult(
                savedAudit.getId(),
                savedAudit.getImportedAt(),
                fetched.size(),
                prsCreated,
                prsUpdated,
                linksUpdated,
                errors);
    }

    private int[] upsertPrSyncRecords(
            String repoSlug, List<GitHubPullRequestData> fetched, Instant fetchedAt, List<SyncError> errors) {
        int prsCreated = 0;
        int prsUpdated = 0;

        for (GitHubPullRequestData pr : fetched) {
            try {
                var existing = prSyncRepository.findByRepoAndPrNumber(repoSlug, pr.number());
                if (existing.isPresent()) {
                    updateExistingPrSync(existing.get(), pr, fetchedAt);
                    prsUpdated++;
                } else {
                    createNewPrSync(repoSlug, pr, fetchedAt);
                    prsCreated++;
                }
            } catch (RuntimeException e) {
                log.warn("github_pr_sync_failed: pr={} error={}", pr.number(), e.getMessage());
                errors.add(new SyncError("upsert", pr.number(), null, e.getMessage()));
            }
        }
        return new int[] {prsCreated, prsUpdated};
    }

    private void updateExistingPrSync(GitHubPullRequestSync sync, GitHubPullRequestData pr, Instant fetchedAt) {
        sync.setPrTitle(pr.title());
        sync.setPrState(parsePrState(pr.state(), pr.merged()));
        sync.setPrBody(pr.body() != null ? pr.body() : "");
        sync.setBaseBranch(pr.baseBranch() != null ? pr.baseBranch() : "");
        sync.setHeadBranch(pr.headBranch() != null ? pr.headBranch() : "");
        sync.setPrLabels(pr.labels());
        sync.setLastFetchedAt(fetchedAt);
        prSyncRepository.save(sync);
    }

    private void createNewPrSync(String repoSlug, GitHubPullRequestData pr, Instant fetchedAt) {
        var sync = new GitHubPullRequestSync(
                repoSlug, pr.number(), pr.title(), parsePrState(pr.state(), pr.merged()), pr.url(), fetchedAt);
        sync.setPrBody(pr.body() != null ? pr.body() : "");
        sync.setBaseBranch(pr.baseBranch() != null ? pr.baseBranch() : "");
        sync.setHeadBranch(pr.headBranch() != null ? pr.headBranch() : "");
        sync.setPrLabels(pr.labels());
        prSyncRepository.save(sync);
    }

    private int updatePrTraceabilityLinks(String repoSlug, Instant fetchedAt, List<SyncError> errors) {
        int linksUpdated = 0;
        var links = traceabilityLinkRepository.findByArtifactType(ArtifactType.PULL_REQUEST);
        for (var link : links) {
            try {
                int prNumber = Integer.parseInt(link.getArtifactIdentifier());
                var syncOpt = prSyncRepository.findByRepoAndPrNumber(repoSlug, prNumber);
                if (syncOpt.isPresent()) {
                    var sync = syncOpt.get();
                    link.setArtifactUrl(sync.getPrUrl());
                    String stateTag = sync.getPrState() != null ? " [" + sync.getPrState() + "]" : "";
                    link.setArtifactTitle("#" + sync.getPrNumber() + " - " + sync.getPrTitle() + stateTag);
                    link.setSyncStatus(SyncStatus.SYNCED);
                    link.setLastSyncedAt(fetchedAt);
                    traceabilityLinkRepository.save(link);
                    linksUpdated++;
                }
            } catch (RuntimeException e) {
                log.warn(
                        "pr_traceability_link_update_failed: artifact={} error={}",
                        link.getArtifactIdentifier(),
                        e.getMessage());
                errors.add(new SyncError("traceability", null, link.getArtifactIdentifier(), e.getMessage()));
            }
        }
        return linksUpdated;
    }

    PullRequestState parsePrState(String state, boolean merged) {
        if (merged) {
            return PullRequestState.MERGED;
        }
        if (state == null) {
            return PullRequestState.OPEN;
        }
        try {
            return PullRequestState.valueOf(state);
        } catch (IllegalArgumentException e) {
            log.warn("github_unknown_pr_state: state={}, defaulting to OPEN", state);
            return PullRequestState.OPEN;
        }
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
                    ArtifactType.GITHUB_ISSUE, String.valueOf(issue.number()), issue.url(), title, LinkType.IMPLEMENTS);
            var link = traceabilityService.createLinkUnchecked(requirement.getId(), linkCommand);
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

    IssueState parseIssueState(String state) {
        if (state == null) {
            return IssueState.OPEN;
        }
        try {
            return IssueState.valueOf(state);
        } catch (IllegalArgumentException e) {
            log.warn("github_unknown_issue_state: state={}, defaulting to OPEN", state);
            return IssueState.OPEN;
        }
    }
}
