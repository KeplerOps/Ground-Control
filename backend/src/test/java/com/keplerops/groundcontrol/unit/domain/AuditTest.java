package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.model.AuditPhase;
import com.keplerops.groundcontrol.domain.audits.state.AuditPhaseKind;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditTest {

    private static final Project PROJECT = makeProject();

    private static Project makeProject() {
        var p = new Project("ground-control", "Ground Control");
        com.keplerops.groundcontrol.TestUtil.setField(p, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return p;
    }

    @Test
    void newAuditStartsPlanned() {
        var a = sample();
        assertThat(a.getStatus()).isEqualTo(AuditStatus.PLANNED);
    }

    @Test
    void coreFieldsPersistThroughConstructor() {
        var a = sample();
        assertThat(a.getProject()).isEqualTo(PROJECT);
        assertThat(a.getUid()).isEqualTo("AUDIT-001");
        assertThat(a.getTitle()).isEqualTo("Annual compliance audit");
        assertThat(a.getAuditType()).isEqualTo(AuditType.INTERNAL);
        assertThat(a.getScopeDescription()).isEqualTo("All production systems.");
        assertThat(a.getObjectives()).isEmpty();
        assertThat(a.getPhases()).isEmpty();
        assertThat(a.getTeamMembers()).isEmpty();
        assertThat(a.getCreatedBy()).isNull();
    }

    @Test
    void mutableOptionalFieldsWork() {
        var a = sample();
        a.setObjectives(List.of("Assess controls", "Review policies"));
        var phase = new AuditPhase(
                AuditPhaseKind.PLANNING, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), null, null);
        a.setPhases(List.of(phase));
        a.setTeamMembers(List.of("alice", "bob"));
        a.setCreatedBy("analyst");

        assertThat(a.getObjectives()).containsExactly("Assess controls", "Review policies");
        assertThat(a.getPhases()).hasSize(1);
        assertThat(a.getPhases().get(0).kind()).isEqualTo(AuditPhaseKind.PLANNING);
        assertThat(a.getTeamMembers()).containsExactly("alice", "bob");
        assertThat(a.getCreatedBy()).isEqualTo("analyst");
    }

    @Test
    void transitionStatusRejectsNullTarget() {
        var a = sample();
        assertThatThrownBy(() -> a.transitionStatus(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void invalidTransitionExceptionCarriesDetailMap() {
        var a = sample();
        assertThatThrownBy(() -> a.transitionStatus(AuditStatus.CLOSED))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(thrown -> {
                    var ex = (DomainValidationException) thrown;
                    assertThat(ex.getDetail()).containsEntry("current_status", "PLANNED");
                    assertThat(ex.getDetail()).containsEntry("target_status", "CLOSED");
                    assertThat(ex.getDetail()).containsKey("valid_targets");
                });
    }

    @Test
    void transitionStatusAdvancesThroughLifecycle() {
        var a = sample();
        a.transitionStatus(AuditStatus.IN_PROGRESS);
        a.transitionStatus(AuditStatus.DRAFT_REPORT);
        a.transitionStatus(AuditStatus.FINAL_REPORT);
        a.transitionStatus(AuditStatus.CLOSED);
        assertThat(a.getStatus()).isEqualTo(AuditStatus.CLOSED);
    }

    @Test
    void finalReportCanReworkToDraftReport() {
        var a = sample();
        a.transitionStatus(AuditStatus.IN_PROGRESS);
        a.transitionStatus(AuditStatus.DRAFT_REPORT);
        a.transitionStatus(AuditStatus.FINAL_REPORT);
        a.transitionStatus(AuditStatus.DRAFT_REPORT);
        assertThat(a.getStatus()).isEqualTo(AuditStatus.DRAFT_REPORT);
    }

    @Test
    void closedIsTerminal() {
        var a = sample();
        a.transitionStatus(AuditStatus.IN_PROGRESS);
        a.transitionStatus(AuditStatus.DRAFT_REPORT);
        a.transitionStatus(AuditStatus.FINAL_REPORT);
        a.transitionStatus(AuditStatus.CLOSED);
        for (AuditStatus target : AuditStatus.values()) {
            assertThatThrownBy(() -> a.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
        }
    }

    private Audit sample() {
        return new Audit(
                PROJECT, "AUDIT-001", "Annual compliance audit", AuditType.INTERNAL, "All production systems.");
    }
}
