package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestSuiteTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    @Nested
    class Construction {

        @Test
        void rejectsNullProject() {
            assertThatThrownBy(() -> new TestSuite(null, "TS-001", "name", TestSuitePopulationMode.STATIC))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Project");
        }

        @Test
        void rejectsBlankUid() {
            assertThatThrownBy(() -> new TestSuite(project(), "   ", "name", TestSuitePopulationMode.STATIC))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("UID");
        }

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> new TestSuite(project(), "TS-001", "", TestSuitePopulationMode.STATIC))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Name");
        }

        @Test
        void rejectsNullPopulationMode() {
            assertThatThrownBy(() -> new TestSuite(project(), "TS-001", "name", null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Population mode");
        }

        @Test
        void storesAllFieldsWhenValid() {
            var suite = new TestSuite(project(), "TS-001", "Wave-1 selection", TestSuitePopulationMode.STATIC);
            assertThat(suite.getUid()).isEqualTo("TS-001");
            assertThat(suite.getName()).isEqualTo("Wave-1 selection");
            assertThat(suite.getPopulationMode()).isEqualTo(TestSuitePopulationMode.STATIC);
            assertThat(suite.getProject()).isNotNull();
            assertThat(suite.hasAnyCriteria()).isFalse();
        }
    }

    @Nested
    class PopulationModeImmutability {

        @Test
        void hasNoSetter() {
            // The compiled API surface is the invariant: there must be no
            // setPopulationMode(). Reflectively reject the method to keep a
            // future "convenience" setter from landing without a code review.
            var hasSetter = java.util.Arrays.stream(TestSuite.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("setPopulationMode"));
            assertThat(hasSetter).isFalse();
        }
    }

    @Nested
    class StaticSuiteCriteria {

        @Test
        void rejectsCriteriaStatusOnStaticSuite() {
            var suite = new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.STATIC);
            assertThatThrownBy(() -> suite.setCriteriaStatus(TestCaseStatus.APPROVED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void acceptsNullCriteriaStatusOnStaticSuiteSoClearWorks() {
            var suite = new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.STATIC);
            suite.setCriteriaStatus(null);
            assertThat(suite.getCriteriaStatus()).isNull();
        }
    }

    @Nested
    class RequirementsBasedSuiteCriteria {

        @Test
        void rejectsCriteriaTypeOnRequirementsBasedSuite() {
            var suite = new TestSuite(project(), "TS-002", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
            assertThatThrownBy(() -> suite.setCriteriaType(TestCaseType.AUTOMATED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void rejectsCriteriaPriorityOnRequirementsBasedSuite() {
            var suite = new TestSuite(project(), "TS-002", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
            assertThatThrownBy(() -> suite.setCriteriaPriority(TestCasePriority.HIGH))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void rejectsCriteriaFormatOnRequirementsBasedSuite() {
            var suite = new TestSuite(project(), "TS-002", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
            assertThatThrownBy(() -> suite.setCriteriaFormat(TestCaseFormat.STEP_BASED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void rejectsCriteriaFolderIdOnRequirementsBasedSuite() {
            var suite = new TestSuite(project(), "TS-002", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
            assertThatThrownBy(() -> suite.setCriteriaFolderId(UUID.randomUUID()))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }

        @Test
        void rejectsCriteriaTextSearchOnRequirementsBasedSuite() {
            var suite = new TestSuite(project(), "TS-002", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
            assertThatThrownBy(() -> suite.setCriteriaTextSearch("hello"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("QUERY_BASED");
        }
    }

    @Nested
    class QueryBasedSuiteCriteria {

        @Test
        void acceptsEveryCriteriaField() {
            var suite = new TestSuite(project(), "TS-003", "n", TestSuitePopulationMode.QUERY_BASED);
            suite.setCriteriaStatus(TestCaseStatus.APPROVED);
            suite.setCriteriaType(TestCaseType.AUTOMATED);
            suite.setCriteriaPriority(TestCasePriority.HIGH);
            suite.setCriteriaFormat(TestCaseFormat.STEP_BASED);
            UUID folderId = UUID.randomUUID();
            suite.setCriteriaFolderId(folderId);
            suite.setCriteriaTextSearch("payment");
            assertThat(suite.getCriteriaStatus()).isEqualTo(TestCaseStatus.APPROVED);
            assertThat(suite.getCriteriaType()).isEqualTo(TestCaseType.AUTOMATED);
            assertThat(suite.getCriteriaPriority()).isEqualTo(TestCasePriority.HIGH);
            assertThat(suite.getCriteriaFormat()).isEqualTo(TestCaseFormat.STEP_BASED);
            assertThat(suite.getCriteriaFolderId()).isEqualTo(folderId);
            assertThat(suite.getCriteriaTextSearch()).isEqualTo("payment");
            assertThat(suite.hasAnyCriteria()).isTrue();
        }

        @Test
        void hasAnyCriteriaReportsFalseForBlankTextOnly() {
            var suite = new TestSuite(project(), "TS-003", "n", TestSuitePopulationMode.QUERY_BASED);
            suite.setCriteriaTextSearch("   ");
            assertThat(suite.hasAnyCriteria()).isFalse();
        }
    }

    @Nested
    class NameAndDescriptionMutability {

        @Test
        void setNameRejectsBlank() {
            var suite = new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.STATIC);
            assertThatThrownBy(() -> suite.setName("")).isInstanceOf(DomainValidationException.class);
        }

        @Test
        void setDescriptionAllowsNull() {
            var suite = new TestSuite(project(), "TS-001", "n", TestSuitePopulationMode.STATIC);
            suite.setDescription("scope");
            suite.setDescription(null);
            assertThat(suite.getDescription()).isNull();
        }
    }
}
