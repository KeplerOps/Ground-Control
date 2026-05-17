package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TestPlanTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    @Test
    void constructorInitialisesRequiredFields() {
        var plan = new TestPlan(project(), "TP-001", "Wave-1 acceptance");
        assertThat(plan.getProject().getIdentifier()).isEqualTo("ground-control");
        assertThat(plan.getUid()).isEqualTo("TP-001");
        assertThat(plan.getName()).isEqualTo("Wave-1 acceptance");
        assertThat(plan.getStatus()).isEqualTo(TestPlanStatus.DRAFT);
        assertThat(plan.getDescription()).isNull();
        assertThat(plan.getProduct()).isNull();
        assertThat(plan.getVersion()).isNull();
        assertThat(plan.getBuild()).isNull();
        assertThat(plan.getStartDate()).isNull();
        assertThat(plan.getEndDate()).isNull();
    }

    @Test
    void constructorRejectsNullProject() {
        assertThatThrownBy(() -> new TestPlan(null, "TP-001", "Plan"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void constructorRejectsBlankUid() {
        var project = project();
        assertThatThrownBy(() -> new TestPlan(project, "", "Plan"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("UID");
        assertThatThrownBy(() -> new TestPlan(project, "   ", "Plan")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestPlan(project, null, "Plan")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsBlankName() {
        var project = project();
        assertThatThrownBy(() -> new TestPlan(project, "TP-001", ""))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Name");
        assertThatThrownBy(() -> new TestPlan(project, "TP-001", "   ")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestPlan(project, "TP-001", null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setNameRejectsBlank() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        assertThatThrownBy(() -> plan.setName(""))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Name");
        assertThatThrownBy(() -> plan.setName(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void setNameAcceptsValidValue() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setName("Renamed plan");
        assertThat(plan.getName()).isEqualTo("Renamed plan");
    }

    @Test
    void setDescriptionAcceptsNullOrText() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setDescription("Initial scope");
        assertThat(plan.getDescription()).isEqualTo("Initial scope");
        plan.setDescription(null);
        assertThat(plan.getDescription()).isNull();
    }

    @Test
    void setProductVersionBuildAcceptsNullOrText() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setProduct("ground-control");
        plan.setVersion("1.2.0");
        plan.setBuild("20260517-abc123");
        assertThat(plan.getProduct()).isEqualTo("ground-control");
        assertThat(plan.getVersion()).isEqualTo("1.2.0");
        assertThat(plan.getBuild()).isEqualTo("20260517-abc123");
        plan.setProduct(null);
        plan.setVersion(null);
        plan.setBuild(null);
        assertThat(plan.getProduct()).isNull();
        assertThat(plan.getVersion()).isNull();
        assertThat(plan.getBuild()).isNull();
    }

    @Test
    void setStartAndEndDatesAcceptOrderedPair() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 15));
        assertThat(plan.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(plan.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void setStartDateEqualToEndDateIsAllowed() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 1));
        assertThat(plan.getEndDate()).isEqualTo(plan.getStartDate());
    }

    @Test
    void setEndDateBeforeStartDateIsRejected() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setStartDate(LocalDate.of(2026, 6, 10));
        assertThatThrownBy(() -> plan.setEndDate(LocalDate.of(2026, 6, 1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("End date");
    }

    @Test
    void setStartDateAfterEndDateIsRejected() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setEndDate(LocalDate.of(2026, 6, 10));
        assertThatThrownBy(() -> plan.setStartDate(LocalDate.of(2026, 6, 20)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Start date");
    }

    @Test
    void datesCanBeClearedIndividually() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 15));
        plan.setStartDate(null);
        assertThat(plan.getStartDate()).isNull();
        assertThat(plan.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        plan.setEndDate(null);
        assertThat(plan.getEndDate()).isNull();
    }

    @Test
    void transitionStatusAdvancesThroughLifecycle() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.transitionStatus(TestPlanStatus.ACTIVE);
        assertThat(plan.getStatus()).isEqualTo(TestPlanStatus.ACTIVE);
        plan.transitionStatus(TestPlanStatus.IN_PROGRESS);
        assertThat(plan.getStatus()).isEqualTo(TestPlanStatus.IN_PROGRESS);
        plan.transitionStatus(TestPlanStatus.COMPLETED);
        assertThat(plan.getStatus()).isEqualTo(TestPlanStatus.COMPLETED);
        plan.transitionStatus(TestPlanStatus.ARCHIVED);
        assertThat(plan.getStatus()).isEqualTo(TestPlanStatus.ARCHIVED);
    }

    @Test
    void transitionStatusRejectsIllegalTarget() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        assertThatThrownBy(() -> plan.transitionStatus(TestPlanStatus.COMPLETED))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void transitionStatusRejectsNullTarget() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        assertThatThrownBy(() -> plan.transitionStatus(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void archivedPlanCannotTransitionAnywhere() {
        var plan = new TestPlan(project(), "TP-001", "Plan");
        plan.transitionStatus(TestPlanStatus.ARCHIVED);
        for (TestPlanStatus target : TestPlanStatus.values()) {
            assertThatThrownBy(() -> plan.transitionStatus(target))
                    .as("ARCHIVED -> %s", target)
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
