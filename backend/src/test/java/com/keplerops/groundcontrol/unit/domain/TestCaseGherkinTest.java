package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import org.junit.jupiter.api.Test;

class TestCaseGherkinTest {

    private TestCase makeTestCase() {
        return new TestCase(
                new Project("ground-control", "Ground Control"),
                "TC-G01",
                "Sign in",
                TestCaseType.MANUAL,
                TestCasePriority.HIGH,
                TestCaseFormat.GHERKIN);
    }

    @Test
    void constructorRejectsNullTestCase() {
        assertThatThrownBy(() -> new TestCaseGherkin(null, "Feature: x\nScenario: y\nGiven z"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankSource() {
        var tc = makeTestCase();
        assertThatThrownBy(() -> new TestCaseGherkin(tc, "")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestCaseGherkin(tc, "   ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setSourceRejectsBlank() {
        var tc = makeTestCase();
        var gherkin = new TestCaseGherkin(tc, "Feature: x\nScenario: y\nGiven z");
        assertThatThrownBy(() -> gherkin.setSource("")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setSourceUpdatesStoredValue() {
        // Positive guard at the model boundary: a future setSource refactor
        // that validated the argument but no-op'd on the write would still
        // pass the rejection test. Assert the write actually lands.
        var tc = makeTestCase();
        var gherkin = new TestCaseGherkin(tc, "Feature: original");
        gherkin.setSource("Feature: updated\nScenario: y\nGiven z");
        assertThat(gherkin.getSource()).isEqualTo("Feature: updated\nScenario: y\nGiven z");
    }

    @Test
    void getSourceReturnsConstructorValue() {
        var tc = makeTestCase();
        var gherkin = new TestCaseGherkin(tc, "Feature: x");
        assertThat(gherkin.getSource()).isEqualTo("Feature: x");
        assertThat(gherkin.getTestCase()).isSameAs(tc);
    }
}
