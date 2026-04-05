package com.keplerops.groundcontrol.domain.requirements.model;

import com.keplerops.groundcontrol.domain.requirements.state.PullRequestState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cached state of a GitHub pull request for sync tracking.
 */
@Entity
@Table(name = "github_pr_sync")
public class GitHubPullRequestSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "pr_number", unique = true, nullable = false)
    private Integer prNumber;

    @Column(name = "pr_title", nullable = false, length = 500)
    private String prTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "pr_state", nullable = false, length = 10)
    private PullRequestState prState;

    @Column(name = "pr_url", nullable = false, length = 2000)
    private String prUrl;

    @Column(name = "pr_body", columnDefinition = "TEXT")
    private String prBody = "";

    @Column(name = "base_branch", length = 255)
    private String baseBranch = "";

    @Column(name = "head_branch", length = 255)
    private String headBranch = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pr_labels", columnDefinition = "jsonb")
    private List<String> prLabels = new ArrayList<>();

    @Column(name = "last_fetched_at", nullable = false)
    private Instant lastFetchedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GitHubPullRequestSync() {
        // JPA
    }

    public GitHubPullRequestSync(
            Integer prNumber, String prTitle, PullRequestState prState, String prUrl, Instant lastFetchedAt) {
        this.prNumber = prNumber;
        this.prTitle = prTitle;
        this.prState = prState;
        this.prUrl = prUrl;
        this.lastFetchedAt = lastFetchedAt;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public PullRequestState getPrState() {
        return prState;
    }

    public void setPrState(PullRequestState prState) {
        this.prState = prState;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public String getPrBody() {
        return prBody;
    }

    public void setPrBody(String prBody) {
        this.prBody = prBody;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public void setHeadBranch(String headBranch) {
        this.headBranch = headBranch;
    }

    public List<String> getPrLabels() {
        return prLabels;
    }

    public void setPrLabels(List<String> prLabels) {
        this.prLabels = prLabels;
    }

    public Instant getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(Instant lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
