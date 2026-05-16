package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import org.junit.jupiter.api.Test;

class TestCaseStepTest {

    private static TestCase testCase() {
        var project = new Project("ground-control", "Ground Control");
        return new TestCase(project, "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
    }

    @Test
    void constructorInitialisesRequiredFields() {
        var step = new TestCaseStep(testCase(), 1, "Open the login page", "Page renders with email field");
        assertThat(step.getStepNumber()).isEqualTo(1);
        assertThat(step.getAction()).isEqualTo("Open the login page");
        assertThat(step.getExpectedResult()).isEqualTo("Page renders with email field");
        assertThat(step.getActualResult()).isNull();
    }

    @Test
    void constructorRejectsNullTestCase() {
        assertThatThrownBy(() -> new TestCaseStep(null, 1, "act", "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Test case");
    }

    @Test
    void constructorRejectsNonPositiveStepNumber() {
        var tc = testCase();
        assertThatThrownBy(() -> new TestCaseStep(tc, 0, "act", "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Step number");
        assertThatThrownBy(() -> new TestCaseStep(tc, -1, "act", "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Step number");
    }

    @Test
    void constructorRejectsBlankAction() {
        var tc = testCase();
        assertThatThrownBy(() -> new TestCaseStep(tc, 1, "", "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Action");
        assertThatThrownBy(() -> new TestCaseStep(tc, 1, "   ", "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Action");
        assertThatThrownBy(() -> new TestCaseStep(tc, 1, null, "exp"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Action");
    }

    @Test
    void constructorRejectsBlankExpectedResult() {
        var tc = testCase();
        assertThatThrownBy(() -> new TestCaseStep(tc, 1, "act", ""))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Expected result");
        assertThatThrownBy(() -> new TestCaseStep(tc, 1, "act", null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Expected result");
    }

    @Test
    void setStepNumberRejectsNonPositive() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        assertThatThrownBy(() -> step.setStepNumber(0)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> step.setStepNumber(-3)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setStepNumberUpdatesValue() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        step.setStepNumber(5);
        assertThat(step.getStepNumber()).isEqualTo(5);
    }

    @Test
    void setActionRejectsBlank() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        assertThatThrownBy(() -> step.setAction("")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> step.setAction("   ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> step.setAction(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setExpectedResultRejectsBlank() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        assertThatThrownBy(() -> step.setExpectedResult("")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> step.setExpectedResult(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setActualResultAcceptsNullAndBlank() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        step.setActualResult("Observed: redirected to /dashboard");
        assertThat(step.getActualResult()).isEqualTo("Observed: redirected to /dashboard");
        step.setActualResult(null);
        assertThat(step.getActualResult()).isNull();
        step.setActualResult("");
        assertThat(step.getActualResult()).isEmpty();
    }

    @Test
    void setActionPreservesRichTextMarkdown() {
        var step = new TestCaseStep(testCase(), 1, "act", "exp");
        var richText = "## Step 1\n\nClick the **Login** button.\n\n![screenshot](https://example.com/login.png)";
        step.setAction(richText);
        assertThat(step.getAction()).isEqualTo(richText);
    }
}
