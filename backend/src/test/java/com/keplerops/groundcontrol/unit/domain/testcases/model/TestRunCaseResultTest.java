package com.keplerops.groundcontrol.unit.domain.testcases.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunCaseResult;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import org.junit.jupiter.api.Test;

class TestRunCaseResultTest {

    private TestRun newRun() {
        var project = new Project("ground-control", "Ground Control");
        var plan = new TestPlan(project, "TP-001", "Wave-1");
        var suite = new TestSuite(project, "TS-001", "Smoke", TestSuitePopulationMode.STATIC);
        return new TestRun(project, plan, suite, "TR-001", "Run");
    }

    private TestCase newTestCase() {
        var project = new Project("ground-control", "Ground Control");
        return new TestCase(project, "TC-001", "Authored case", TestCaseType.MANUAL, TestCasePriority.MEDIUM);
    }

    @Test
    void constructorAcceptsValidArgsAndDefaultsToNotRun() {
        var run = newRun();
        var tc = newTestCase();
        var result = new TestRunCaseResult(run, tc, "TC-001", "Authored case", 0);
        assertThat(result.getTestRun()).isSameAs(run);
        assertThat(result.getTestCase()).isSameAs(tc);
        assertThat(result.getTestCaseUid()).isEqualTo("TC-001");
        assertThat(result.getTestCaseTitle()).isEqualTo("Authored case");
        assertThat(result.getStatus()).isEqualTo(TestRunCaseResultStatus.NOT_RUN);
    }

    @Test
    void constructorRejectsNullRun() {
        var tc = newTestCase();
        assertThatThrownBy(() -> new TestRunCaseResult(null, tc, "TC-001", "Title", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNullTestCase() {
        var run = newRun();
        assertThatThrownBy(() -> new TestRunCaseResult(run, null, "TC-001", "Title", 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankSnapshotFields() {
        var run = newRun();
        var tc = newTestCase();
        assertThatThrownBy(() -> new TestRunCaseResult(run, tc, "", "Title", 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunCaseResult(run, tc, "TC-001", " ", 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunCaseResult(run, tc, null, "Title", 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunCaseResult(run, tc, "TC-001", null, 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStatusRejectsNull() {
        var run = newRun();
        var tc = newTestCase();
        var result = new TestRunCaseResult(run, tc, "TC-001", "Title", 0);
        assertThatThrownBy(() -> result.setStatus(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStatusAcceptsAnyEnumValue() {
        // No transition graph — any status is a legal target. The free
        // flips reflect that testers re-test, descope, or unblock cases
        // over the life of a run.
        var run = newRun();
        var tc = newTestCase();
        var result = new TestRunCaseResult(run, tc, "TC-001", "Title", 0);
        result.setStatus(TestRunCaseResultStatus.FAILED);
        assertThat(result.getStatus()).isEqualTo(TestRunCaseResultStatus.FAILED);
        result.setStatus(TestRunCaseResultStatus.PASSED);
        assertThat(result.getStatus()).isEqualTo(TestRunCaseResultStatus.PASSED);
        result.setStatus(TestRunCaseResultStatus.BLOCKED);
        assertThat(result.getStatus()).isEqualTo(TestRunCaseResultStatus.BLOCKED);
    }

    @Test
    void constructorRejectsNegativeSnapshotOrder() {
        var run = newRun();
        var tc = newTestCase();
        assertThatThrownBy(() -> new TestRunCaseResult(run, tc, "TC-001", "Title", -1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Snapshot order");
    }

    @Test
    void snapshotOrderPreservesResolvedIndex() {
        var run = newRun();
        var tc = newTestCase();
        var result = new TestRunCaseResult(run, tc, "TC-001", "Title", 5);
        assertThat(result.getSnapshotOrder()).isEqualTo(5);
    }

    @Test
    void notesAreOptional() {
        var run = newRun();
        var tc = newTestCase();
        var result = new TestRunCaseResult(run, tc, "TC-001", "Title", 0);
        var notes = "Failed at step 3: button missing on prod 1.2.0 build 42";
        result.setNotes(notes);
        // Exact equality — a setter regression that trimmed, wrapped, or
        // template-decorated the value would be invisible to a `contains`
        // substring assertion.
        assertThat(result.getNotes()).isEqualTo(notes);
        result.setNotes(null);
        assertThat(result.getNotes()).isNull();
    }
}
