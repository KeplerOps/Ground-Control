package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * TC-008 / ADR-049 — Execution-time record for one pass through a
 * {@link TestSuite} against a {@link TestPlan} for a specific
 * environment / version / build window.
 *
 * <p>The aggregate is project-scoped, {@code @Audited}, and carries
 * release-coordinate scalars (environment / version / build) consistent
 * with {@link TestPlan}'s precedent. Assigned testers and per-case results
 * are normalized into child rows ({@link TestRunTesterAssignment} and
 * {@link TestRunCaseResult}) so they are queryable, validated, and audited
 * — never comma-separated text or unbounded JSON.
 *
 * <p>Plan and suite are required and validated at the service layer for
 * project ownership. Plan / suite references are {@code @NotAudited} on
 * the JPA mapping; their FKs are intentionally absent from the audit
 * shadow, mirroring the test-plan / test-suite shadow shape.
 */
@Entity
@Audited
@Table(name = "test_run", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class TestRun extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "test_plan_id", nullable = false)
    private TestPlan testPlan;

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String environment;

    @Column(length = 100)
    private String version;

    @Column(length = 100)
    private String build;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestRunStatus status = TestRunStatus.PLANNED;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    protected TestRun() {
        // JPA
    }

    public TestRun(Project project, TestPlan testPlan, TestSuite testSuite, String uid, String name) {
        if (project == null) {
            throw new DomainValidationException("Project must not be null", "invalid_test_run", Map.of());
        }
        if (testPlan == null) {
            throw new DomainValidationException("Test plan must not be null", "invalid_test_run", Map.of());
        }
        if (testSuite == null) {
            throw new DomainValidationException("Test suite must not be null", "invalid_test_run", Map.of());
        }
        if (uid == null || uid.isBlank()) {
            throw new DomainValidationException("UID must not be blank", "invalid_test_run", Map.of());
        }
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_run", Map.of());
        }
        this.project = project;
        this.testPlan = testPlan;
        this.testSuite = testSuite;
        this.uid = uid;
        this.name = name;
    }

    public void transitionStatus(TestRunStatus newStatus) {
        if (newStatus == null || !status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition test run status from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current", status.name(), "requested", String.valueOf(newStatus)));
        }
        this.status = newStatus;
    }

    public Project getProject() {
        return project;
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_run", Map.of());
        }
        this.name = name;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public TestRunStatus getStatus() {
        return status;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        if (startAt != null && this.endAt != null && startAt.isAfter(this.endAt)) {
            throw new DomainValidationException(
                    "start_at must be on or before end_at",
                    "invalid_test_run_window",
                    Map.of("start_at", startAt.toString(), "end_at", this.endAt.toString()));
        }
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        if (endAt != null && this.startAt != null && endAt.isBefore(this.startAt)) {
            throw new DomainValidationException(
                    "end_at must be on or after start_at",
                    "invalid_test_run_window",
                    Map.of("start_at", this.startAt.toString(), "end_at", endAt.toString()));
        }
        this.endAt = endAt;
    }
}
