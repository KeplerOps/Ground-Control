package com.keplerops.groundcontrol.unit.domain.testcases.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunTesterAssignment;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import org.junit.jupiter.api.Test;

class TestRunTesterAssignmentTest {

    private TestRun newRun() {
        var project = new Project("ground-control", "Ground Control");
        var plan = new TestPlan(project, "TP-001", "Wave-1");
        var suite = new TestSuite(project, "TS-001", "Smoke", TestSuitePopulationMode.STATIC);
        return new TestRun(project, plan, suite, "TR-001", "Run");
    }

    @Test
    void constructorAcceptsValidArgs() {
        var run = newRun();
        var assignment = new TestRunTesterAssignment(run, "Alex Doe");
        assertThat(assignment.getTestRun()).isSameAs(run);
        assertThat(assignment.getTesterName()).isEqualTo("Alex Doe");
    }

    @Test
    void constructorRejectsNullRun() {
        assertThatThrownBy(() -> new TestRunTesterAssignment(null, "Alex"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankTesterName() {
        var run = newRun();
        assertThatThrownBy(() -> new TestRunTesterAssignment(run, "")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunTesterAssignment(run, "   ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestRunTesterAssignment(run, null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsPathReservedCharacters() {
        // Tester names round-trip through `/testers/{testerName}` as a URL
        // path segment on remove; reject characters that don't survive
        // path-segment encode/decode so every name we accept can be deleted.
        var run = newRun();
        for (String invalid : new String[] {
            "alex/jones", "alex?q=1", "alex#tag", "alex%20jones", "alex\\jones", "alex<jones", "alex>jones"
        }) {
            assertThatThrownBy(() -> new TestRunTesterAssignment(run, invalid))
                    .as("invalid input: %s", invalid)
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("unsupported characters");
        }
    }

    @Test
    void constructorAcceptsTypicalHumanNames() {
        var run = newRun();
        for (String valid :
                new String[] {"Alex", "Alex Doe", "Mary O'Connor", "alex@example.com", "user_42", "Jean-Luc Picard"}) {
            var assignment = new TestRunTesterAssignment(run, valid);
            assertThat(assignment.getTesterName()).isEqualTo(valid);
        }
    }
}
