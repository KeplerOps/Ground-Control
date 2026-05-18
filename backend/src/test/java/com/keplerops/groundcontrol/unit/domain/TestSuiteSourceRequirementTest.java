package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteSourceRequirement;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import org.junit.jupiter.api.Test;

class TestSuiteSourceRequirementTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    private static TestSuite suite() {
        return new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
    }

    private static Requirement requirement() {
        return new Requirement(project(), "REQ-001", "title", "statement");
    }

    @Test
    void constructorRejectsNullSuite() {
        var req = requirement();
        assertThatThrownBy(() -> new TestSuiteSourceRequirement(null, req))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Test suite");
    }

    @Test
    void constructorRejectsNullRequirement() {
        var s = suite();
        assertThatThrownBy(() -> new TestSuiteSourceRequirement(s, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Requirement");
    }

    @Test
    void constructorStoresFields() {
        var s = suite();
        var req = requirement();
        var source = new TestSuiteSourceRequirement(s, req);
        assertThat(source.getTestSuite()).isSameAs(s);
        assertThat(source.getRequirement()).isSameAs(req);
    }
}
