package com.keplerops.groundcontrol.unit.domain.testcases.model;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunCaseResult;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunStepResult;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TC-009 / ADR-050 — Invariants for the {@link TestRunStepResult} entity.
 * Mirrors the existing {@code TestRunCaseResultTest} pattern: every
 * construction path the service layer can take must be exercised so a
 * silent regression of the constructor guard surfaces at unit-test time,
 * not at production flush.
 */
class TestRunStepResultTest {

    private Project project;
    private TestPlan plan;
    private TestSuite suite;
    private TestRun run;
    private TestCase testCase;
    private TestCaseStep step;
    private TestRunCaseResult caseResult;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        setField(project, "id", UUID.randomUUID());
        plan = new TestPlan(project, "TP-001", "Wave-1");
        setField(plan, "id", UUID.randomUUID());
        suite = new TestSuite(project, "TS-001", "Smoke", TestSuitePopulationMode.STATIC);
        setField(suite, "id", UUID.randomUUID());
        run = new TestRun(project, plan, suite, "TR-001", "Smoke pass 1");
        setField(run, "id", UUID.randomUUID());
        testCase = new TestCase(project, "TC-001", "Login", TestCaseType.MANUAL, TestCasePriority.MEDIUM);
        setField(testCase, "id", UUID.randomUUID());
        step = new TestCaseStep(testCase, 1, "Open login page", "Login form visible");
        setField(step, "id", UUID.randomUUID());
        caseResult = new TestRunCaseResult(run, testCase, "TC-001", "Login", 0);
        setField(caseResult, "id", UUID.randomUUID());
    }

    @Test
    void constructorPopulatesSnapshotFields() {
        var result = new TestRunStepResult(caseResult, step, 1, "Open login page", "Login form visible", 0);

        assertThat(result.getTestRunCaseResult()).isSameAs(caseResult);
        assertThat(result.getTestCaseStep()).isSameAs(step);
        assertThat(result.getStepNumberSnapshot()).isEqualTo(1);
        assertThat(result.getActionSnapshot()).isEqualTo("Open login page");
        assertThat(result.getExpectedResultSnapshot()).isEqualTo("Login form visible");
        assertThat(result.getSnapshotOrder()).isZero();
        // Default status is NOT_RUN — only execution flips it. Mirrors
        // TestRunCaseResultStatus.NOT_RUN default on the parent case.
        assertThat(result.getStatus()).isEqualTo(TestRunCaseResultStatus.NOT_RUN);
        assertThat(result.getComment()).isNull();
        assertThat(result.getExecutedAt()).isNull();
    }

    @Test
    void constructorRejectsNullCaseResult() {
        assertThatThrownBy(() -> new TestRunStepResult(null, step, 1, "a", "b", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNullStep() {
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, null, 1, "a", "b", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNonPositiveStepNumberSnapshot() {
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 0, "a", "b", 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, -1, "a", "b", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankActionSnapshot() {
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 1, null, "b", 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 1, "  ", "b", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankExpectedResultSnapshot() {
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 1, "a", null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 1, "a", "  ", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNegativeSnapshotOrder() {
        assertThatThrownBy(() -> new TestRunStepResult(caseResult, step, 1, "a", "b", -1))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStatusRejectsNull() {
        var result = new TestRunStepResult(caseResult, step, 1, "a", "b", 0);
        assertThatThrownBy(() -> result.setStatus(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStatusAcceptsAllEnumValues() {
        var result = new TestRunStepResult(caseResult, step, 1, "a", "b", 0);
        // No transition graph at the step level — the tester is free to
        // flip between states (PASSED → FAILED on re-test, BLOCKED →
        // SKIPPED on descope, etc.). The enum exhaustively covers the
        // five outcomes.
        for (var status : TestRunCaseResultStatus.values()) {
            result.setStatus(status);
            assertThat(result.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void setCommentAcceptsNullAndArbitraryText() {
        var result = new TestRunStepResult(caseResult, step, 1, "a", "b", 0);
        result.setComment(null);
        assertThat(result.getComment()).isNull();
        result.setComment("Tester noticed the modal flicker");
        assertThat(result.getComment()).isEqualTo("Tester noticed the modal flicker");
    }

    @Test
    void setExecutedAtAcceptsNullAndInstant() {
        var result = new TestRunStepResult(caseResult, step, 1, "a", "b", 0);
        var ts = Instant.parse("2026-06-15T12:00:00Z");
        result.setExecutedAt(ts);
        assertThat(result.getExecutedAt()).isEqualTo(ts);
        result.setExecutedAt(null);
        assertThat(result.getExecutedAt()).isNull();
    }
}
