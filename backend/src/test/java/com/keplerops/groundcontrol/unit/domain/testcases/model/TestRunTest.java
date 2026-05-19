package com.keplerops.groundcontrol.unit.domain.testcases.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestRunTest {

    private Project newProject() {
        return new Project("ground-control", "Ground Control");
    }

    private TestPlan newPlan(Project project) {
        return new TestPlan(project, "TP-001", "Wave-1");
    }

    private TestSuite newSuite(Project project) {
        return new TestSuite(project, "TS-001", "Smoke", TestSuitePopulationMode.STATIC);
    }

    @Test
    void constructorAcceptsValidArgs() {
        var project = newProject();
        var plan = newPlan(project);
        var suite = newSuite(project);
        var run = new TestRun(project, plan, suite, "TR-001", "Smoke pass 1");

        assertThat(run.getProject()).isSameAs(project);
        assertThat(run.getTestPlan()).isSameAs(plan);
        assertThat(run.getTestSuite()).isSameAs(suite);
        assertThat(run.getUid()).isEqualTo("TR-001");
        assertThat(run.getName()).isEqualTo("Smoke pass 1");
        assertThat(run.getStatus()).isEqualTo(TestRunStatus.PLANNED);
    }

    @Test
    void constructorRejectsNullProject() {
        var project = newProject();
        var plan = newPlan(project);
        var suite = newSuite(project);
        assertThatThrownBy(() -> new TestRun(null, plan, suite, "TR-001", "Run"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNullPlan() {
        var project = newProject();
        var suite = newSuite(project);
        assertThatThrownBy(() -> new TestRun(project, null, suite, "TR-001", "Run"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNullSuite() {
        var project = newProject();
        var plan = newPlan(project);
        assertThatThrownBy(() -> new TestRun(project, plan, null, "TR-001", "Run"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankUid() {
        var project = newProject();
        var plan = newPlan(project);
        var suite = newSuite(project);
        assertThatThrownBy(() -> new TestRun(project, plan, suite, "  ", "Run"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankName() {
        var project = newProject();
        var plan = newPlan(project);
        var suite = newSuite(project);
        assertThatThrownBy(() -> new TestRun(project, plan, suite, "TR-001", ""))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setNameRejectsBlank() {
        var run = newRun();
        assertThatThrownBy(() -> run.setName(null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> run.setName("   ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void transitionStatusFollowsTheGraph() {
        var run = newRun();
        run.transitionStatus(TestRunStatus.IN_PROGRESS);
        assertThat(run.getStatus()).isEqualTo(TestRunStatus.IN_PROGRESS);
        run.transitionStatus(TestRunStatus.COMPLETED);
        assertThat(run.getStatus()).isEqualTo(TestRunStatus.COMPLETED);
    }

    @Test
    void transitionStatusRejectsIllegalArc() {
        var run = newRun();
        // PLANNED → COMPLETED is not allowed (must go through IN_PROGRESS).
        assertThatThrownBy(() -> run.transitionStatus(TestRunStatus.COMPLETED))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Cannot transition test run status");
    }

    @Test
    void transitionStatusRejectsNullTarget() {
        var run = newRun();
        assertThatThrownBy(() -> run.transitionStatus(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStartAtRejectsValueAfterEndAt() {
        var run = newRun();
        run.setEndAt(Instant.parse("2026-06-01T00:00:00Z"));
        var invalidStart = Instant.parse("2026-06-02T00:00:00Z");
        assertThatThrownBy(() -> run.setStartAt(invalidStart))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("start_at must be on or before end_at");
    }

    @Test
    void setEndAtRejectsValueBeforeStartAt() {
        var run = newRun();
        run.setStartAt(Instant.parse("2026-06-01T00:00:00Z"));
        var invalidEnd = Instant.parse("2026-05-31T00:00:00Z");
        assertThatThrownBy(() -> run.setEndAt(invalidEnd))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("end_at must be on or after start_at");
    }

    @Test
    void startAtAndEndAtAcceptEqualInstants() {
        var run = newRun();
        var t = Instant.parse("2026-06-01T00:00:00Z");
        run.setStartAt(t);
        run.setEndAt(t);
        assertThat(run.getStartAt()).isEqualTo(t);
        assertThat(run.getEndAt()).isEqualTo(t);
    }

    @Test
    void setStartAtClearsToNullEvenWhenEndAtIsSet() {
        // Clearing one endpoint should always be allowed; only the
        // non-null pair triggers the comparison.
        var run = newRun();
        run.setStartAt(Instant.parse("2026-06-01T00:00:00Z"));
        run.setEndAt(Instant.parse("2026-06-30T00:00:00Z"));
        run.setStartAt(null);
        assertThat(run.getStartAt()).isNull();
    }

    private TestRun newRun() {
        var project = newProject();
        var plan = newPlan(project);
        var suite = newSuite(project);
        return new TestRun(project, plan, suite, "TR-001", "Run");
    }
}
