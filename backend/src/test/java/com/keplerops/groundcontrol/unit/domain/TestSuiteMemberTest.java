package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteMember;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import org.junit.jupiter.api.Test;

class TestSuiteMemberTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    private static TestSuite suite() {
        return new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.STATIC);
    }

    private static TestCase testCase() {
        return new TestCase(project(), "TC-001", "title", TestCaseType.MANUAL, TestCasePriority.MEDIUM);
    }

    @Test
    void constructorRejectsNullSuite() {
        var tc = testCase();
        assertThatThrownBy(() -> new TestSuiteMember(null, tc, 0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Test suite");
    }

    @Test
    void constructorRejectsNullTestCase() {
        var s = suite();
        assertThatThrownBy(() -> new TestSuiteMember(s, null, 0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Test case");
    }

    @Test
    void constructorRejectsNegativePosition() {
        var s = suite();
        var tc = testCase();
        assertThatThrownBy(() -> new TestSuiteMember(s, tc, -1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Position");
    }

    @Test
    void constructorStoresFields() {
        var s = suite();
        var tc = testCase();
        var member = new TestSuiteMember(s, tc, 3);
        assertThat(member.getTestSuite()).isSameAs(s);
        assertThat(member.getTestCase()).isSameAs(tc);
        assertThat(member.getPosition()).isEqualTo(3);
    }

    @Test
    void setPositionRejectsNegative() {
        var member = new TestSuiteMember(suite(), testCase(), 0);
        assertThatThrownBy(() -> member.setPosition(-1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Position");
    }

    @Test
    void setPositionStoresValidValue() {
        var member = new TestSuiteMember(suite(), testCase(), 0);
        member.setPosition(7);
        assertThat(member.getPosition()).isEqualTo(7);
    }
}
