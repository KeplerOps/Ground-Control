package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
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
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * TC-009 / ADR-050 — Snapshotted execution result for one
 * {@link TestCaseStep} inside one {@link TestRunCaseResult}.
 *
 * <p>Created at run-create time by snapshotting the live
 * {@link TestCaseStep}s of every resolved case in
 * {@code TestRunService.create}. The {@code actionSnapshot},
 * {@code expectedResultSnapshot}, and {@code stepNumberSnapshot} fields are
 * authoritative for replay; later edits to the authored step never rewrite
 * a run's historical evidence (ADR-041 keeps {@code TestCaseStep.actualResult}
 * definition-time; runtime evidence lives here).
 *
 * <p>The {@code status} value uses {@link TestRunCaseResultStatus}
 * intentionally — a tester needs the same five outcomes at step level as at
 * case level. A separate parallel enum would split documentation and the
 * frontend enum mirror for no semantic gain (ADR-034 contract).
 *
 * <p>Each {@code (test_run_case_result_id, test_case_step_id)} pair is
 * unique, and so is {@code (test_run_case_result_id, snapshot_order)}.
 */
@Entity
@Audited
@Table(
        name = "test_run_step_result",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_test_run_step_result",
                    columnNames = {"test_run_case_result_id", "test_case_step_id"}),
            @UniqueConstraint(
                    name = "uq_test_run_step_result_order",
                    columnNames = {"test_run_case_result_id", "snapshot_order"})
        })
public class TestRunStepResult extends BaseEntity {

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_case_result_id", nullable = false)
    private TestRunCaseResult testRunCaseResult;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_step_id", nullable = false)
    private TestCaseStep testCaseStep;

    @Column(name = "step_number_snapshot", nullable = false)
    private int stepNumberSnapshot;

    @Column(name = "action_snapshot", nullable = false, columnDefinition = "TEXT")
    private String actionSnapshot;

    @Column(name = "expected_result_snapshot", nullable = false, columnDefinition = "TEXT")
    private String expectedResultSnapshot;

    @Column(name = "snapshot_order", nullable = false)
    private int snapshotOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestRunCaseResultStatus status = TestRunCaseResultStatus.NOT_RUN;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "executed_at")
    private Instant executedAt;

    protected TestRunStepResult() {
        // JPA
    }

    public TestRunStepResult(
            TestRunCaseResult testRunCaseResult,
            TestCaseStep testCaseStep,
            int stepNumberSnapshot,
            String actionSnapshot,
            String expectedResultSnapshot,
            int snapshotOrder) {
        if (testRunCaseResult == null) {
            throw new DomainValidationException(
                    "Test run case result must not be null", "invalid_test_run_step_result", Map.of());
        }
        if (testCaseStep == null) {
            throw new DomainValidationException(
                    "Test case step must not be null", "invalid_test_run_step_result", Map.of());
        }
        if (stepNumberSnapshot <= 0) {
            throw new DomainValidationException(
                    "Step number snapshot must be positive",
                    "invalid_test_run_step_result",
                    Map.of("stepNumberSnapshot", String.valueOf(stepNumberSnapshot)));
        }
        if (actionSnapshot == null || actionSnapshot.isBlank()) {
            throw new DomainValidationException(
                    "Action snapshot must not be blank", "invalid_test_run_step_result", Map.of());
        }
        if (expectedResultSnapshot == null || expectedResultSnapshot.isBlank()) {
            throw new DomainValidationException(
                    "Expected result snapshot must not be blank", "invalid_test_run_step_result", Map.of());
        }
        if (snapshotOrder < 0) {
            throw new DomainValidationException(
                    "Snapshot order must be non-negative", "invalid_test_run_step_result", Map.of());
        }
        this.testRunCaseResult = testRunCaseResult;
        this.testCaseStep = testCaseStep;
        this.stepNumberSnapshot = stepNumberSnapshot;
        this.actionSnapshot = actionSnapshot;
        this.expectedResultSnapshot = expectedResultSnapshot;
        this.snapshotOrder = snapshotOrder;
    }

    public TestRunCaseResult getTestRunCaseResult() {
        return testRunCaseResult;
    }

    public TestCaseStep getTestCaseStep() {
        return testCaseStep;
    }

    public int getStepNumberSnapshot() {
        return stepNumberSnapshot;
    }

    public String getActionSnapshot() {
        return actionSnapshot;
    }

    public String getExpectedResultSnapshot() {
        return expectedResultSnapshot;
    }

    public int getSnapshotOrder() {
        return snapshotOrder;
    }

    public TestRunCaseResultStatus getStatus() {
        return status;
    }

    public void setStatus(TestRunCaseResultStatus status) {
        if (status == null) {
            throw new DomainValidationException("Status must not be null", "invalid_test_run_step_result", Map.of());
        }
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
