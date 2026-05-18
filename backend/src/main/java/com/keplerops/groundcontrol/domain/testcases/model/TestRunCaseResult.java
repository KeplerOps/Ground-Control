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
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * TC-008 / ADR-049 — Snapshotted execution result for one
 * {@link TestCase} inside one {@link TestRun}.
 *
 * <p>This row is the canonical membership of the run: created during run
 * creation by snapshotting the resolved test cases from the run's
 * {@link TestSuite}. Later edits to the source suite or the test case
 * never rewrite this row. The {@code testCaseUid} / {@code testCaseTitle}
 * snapshot fields preserve identification even if the linked test case is
 * renamed.
 *
 * <p>Each {@code (test_run_id, test_case_id)} pair is unique.
 */
@Entity
@Audited
@Table(
        name = "test_run_case_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_run_id", "test_case_id"}))
public class TestRunCaseResult extends BaseEntity {

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(name = "test_case_uid", nullable = false, length = 50)
    private String testCaseUid;

    @Column(name = "test_case_title", nullable = false, length = 200)
    private String testCaseTitle;

    @Column(name = "snapshot_order", nullable = false)
    private int snapshotOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestRunCaseResultStatus status = TestRunCaseResultStatus.NOT_RUN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected TestRunCaseResult() {
        // JPA
    }

    public TestRunCaseResult(
            TestRun testRun, TestCase testCase, String testCaseUid, String testCaseTitle, int snapshotOrder) {
        if (testRun == null) {
            throw new DomainValidationException("Test run must not be null", "invalid_test_run_case_result", Map.of());
        }
        if (testCase == null) {
            throw new DomainValidationException("Test case must not be null", "invalid_test_run_case_result", Map.of());
        }
        if (testCaseUid == null || testCaseUid.isBlank()) {
            throw new DomainValidationException(
                    "Test case UID snapshot must not be blank", "invalid_test_run_case_result", Map.of());
        }
        if (testCaseTitle == null || testCaseTitle.isBlank()) {
            throw new DomainValidationException(
                    "Test case title snapshot must not be blank", "invalid_test_run_case_result", Map.of());
        }
        if (snapshotOrder < 0) {
            throw new DomainValidationException(
                    "Snapshot order must be non-negative", "invalid_test_run_case_result", Map.of());
        }
        this.testRun = testRun;
        this.testCase = testCase;
        this.testCaseUid = testCaseUid;
        this.testCaseTitle = testCaseTitle;
        this.snapshotOrder = snapshotOrder;
    }

    public TestRun getTestRun() {
        return testRun;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public String getTestCaseUid() {
        return testCaseUid;
    }

    public String getTestCaseTitle() {
        return testCaseTitle;
    }

    public int getSnapshotOrder() {
        return snapshotOrder;
    }

    public TestRunCaseResultStatus getStatus() {
        return status;
    }

    public void setStatus(TestRunCaseResultStatus status) {
        if (status == null) {
            throw new DomainValidationException("Status must not be null", "invalid_test_run_case_result", Map.of());
        }
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
