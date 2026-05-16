package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import org.junit.jupiter.api.Test;

class TestCaseTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    @Test
    void constructorInitialisesRequiredFields() {
        var testCase = new TestCase(project(), "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
        assertThat(testCase.getUid()).isEqualTo("TC-001");
        assertThat(testCase.getTitle()).isEqualTo("Login flow");
        assertThat(testCase.getType()).isEqualTo(TestCaseType.MANUAL);
        assertThat(testCase.getPriority()).isEqualTo(TestCasePriority.HIGH);
    }

    @Test
    void constructorDefaultsStatusToDraft() {
        var testCase = new TestCase(project(), "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
        assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.DRAFT);
    }

    @Test
    void constructorRejectsNullProject() {
        assertThatThrownBy(() -> new TestCase(null, "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void constructorRejectsBlankUid() {
        assertThatThrownBy(() -> new TestCase(project(), "", "t", TestCaseType.MANUAL, TestCasePriority.LOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("UID");
        assertThatThrownBy(() -> new TestCase(project(), "   ", "t", TestCaseType.MANUAL, TestCasePriority.LOW))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankTitle() {
        assertThatThrownBy(() -> new TestCase(project(), "TC-001", "", TestCaseType.MANUAL, TestCasePriority.LOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void constructorRejectsNullType() {
        assertThatThrownBy(() -> new TestCase(project(), "TC-001", "t", null, TestCasePriority.LOW))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Type");
    }

    @Test
    void constructorRejectsNullPriority() {
        assertThatThrownBy(() -> new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Priority");
    }

    @Test
    void transitionStatusAdvancesLifecycle() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.AUTOMATED, TestCasePriority.MEDIUM);
        testCase.transitionStatus(TestCaseStatus.APPROVED);
        assertThat(testCase.getStatus()).isEqualTo(TestCaseStatus.APPROVED);
    }

    @Test
    void transitionStatusRejectsInvalidTransition() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.AUTOMATED, TestCasePriority.MEDIUM);
        assertThatThrownBy(() -> testCase.transitionStatus(TestCaseStatus.DEPRECATED))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("DEPRECATED");
    }

    @Test
    void transitionStatusRejectsNull() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.AUTOMATED, TestCasePriority.MEDIUM);
        assertThatThrownBy(() -> testCase.transitionStatus(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setEstimatedDurationSecondsRejectsNegative() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW);
        assertThatThrownBy(() -> testCase.setEstimatedDurationSeconds(-1L))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void setEstimatedDurationSecondsAcceptsZero() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW);
        testCase.setEstimatedDurationSeconds(0L);
        assertThat(testCase.getEstimatedDurationSeconds()).isZero();
    }

    @Test
    void setEstimatedDurationSecondsAcceptsNull() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW);
        testCase.setEstimatedDurationSeconds(600L);
        testCase.setEstimatedDurationSeconds(null);
        assertThat(testCase.getEstimatedDurationSeconds()).isNull();
    }

    @Test
    void richTextFieldsAreSettable() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW);
        testCase.setDescription("# overview");
        testCase.setPreconditions("- logged in");
        testCase.setPostconditions("- session cleared");
        assertThat(testCase.getDescription()).isEqualTo("# overview");
        assertThat(testCase.getPreconditions()).isEqualTo("- logged in");
        assertThat(testCase.getPostconditions()).isEqualTo("- session cleared");
    }

    @Test
    void setTitleRejectsBlank() {
        var testCase = new TestCase(project(), "TC-001", "t", TestCaseType.MANUAL, TestCasePriority.LOW);
        assertThatThrownBy(() -> testCase.setTitle("")).isInstanceOf(DomainValidationException.class);
    }
}
