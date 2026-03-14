package com.keplerops.groundcontrol.domain.requirements.model;

import com.keplerops.groundcontrol.domain.requirements.state.IssueState;
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
 * Cached state of a GitHub issue for sync tracking.
 */
@Entity
@Table(name = "github_issue_sync")
public class GitHubIssueSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "issue_number", unique = true, nullable = false)
    private Integer issueNumber;

    @Column(name = "issue_title", nullable = false, length = 500)
    private String issueTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_state", nullable = false, length = 10)
    private IssueState issueState;

    @Column(name = "issue_url", nullable = false, length = 2000)
    private String issueUrl;

    @Column(name = "issue_body", columnDefinition = "TEXT")
    private String issueBody = "";

    private Integer phase;

    @Column(name = "priority_label", length = 10)
    private String priorityLabel = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issue_labels", columnDefinition = "jsonb")
    private List<String> issueLabels = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cross_references", columnDefinition = "jsonb")
    private List<Integer> crossReferences = new ArrayList<>();

    @Column(name = "last_fetched_at", nullable = false)
    private Instant lastFetchedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GitHubIssueSync() {
        // JPA
    }

    public GitHubIssueSync(
            Integer issueNumber, String issueTitle, IssueState issueState, String issueUrl, Instant lastFetchedAt) {
        this.issueNumber = issueNumber;
        this.issueTitle = issueTitle;
        this.issueState = issueState;
        this.issueUrl = issueUrl;
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

    public Integer getIssueNumber() {
        return issueNumber;
    }

    public String getIssueTitle() {
        return issueTitle;
    }

    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }

    public IssueState getIssueState() {
        return issueState;
    }

    public void setIssueState(IssueState issueState) {
        this.issueState = issueState;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public String getIssueBody() {
        return issueBody;
    }

    public void setIssueBody(String issueBody) {
        this.issueBody = issueBody;
    }

    public Integer getPhase() {
        return phase;
    }

    public void setPhase(Integer phase) {
        this.phase = phase;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public List<String> getIssueLabels() {
        return issueLabels;
    }

    public void setIssueLabels(List<String> issueLabels) {
        this.issueLabels = issueLabels;
    }

    public List<Integer> getCrossReferences() {
        return crossReferences;
    }

    public void setCrossReferences(List<Integer> crossReferences) {
        this.crossReferences = crossReferences;
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
