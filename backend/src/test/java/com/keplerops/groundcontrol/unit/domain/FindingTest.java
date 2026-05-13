package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindingTest {

    private static final Project PROJECT = makeProject();

    private static Project makeProject() {
        var p = new Project("ground-control", "Ground Control");
        com.keplerops.groundcontrol.TestUtil.setField(p, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return p;
    }

    @Test
    void newFindingStartsOpen() {
        var f = sample();
        assertThat(f.getStatus()).isEqualTo(FindingStatus.OPEN);
    }

    @Test
    void coreFieldsPersistThroughConstructor() {
        var f = sample();
        assertThat(f.getProject()).isEqualTo(PROJECT);
        assertThat(f.getUid()).isEqualTo("FIND-001");
        assertThat(f.getTitle()).isEqualTo("MFA missing on admin portal");
        assertThat(f.getFindingType()).isEqualTo(FindingType.CONTROL_DEFICIENCY);
        assertThat(f.getSeverity()).isEqualTo(FindingSeverity.HIGH);
        assertThat(f.getDescription()).isEqualTo("Admin portal accepts password-only auth.");
        assertThat(f.getRootCauseAnalysis()).isNull();
        assertThat(f.getOwner()).isNull();
        assertThat(f.getDueDate()).isNull();
        assertThat(f.getCreatedBy()).isNull();
    }

    @Test
    void optionalFieldsAreMutable() {
        var f = sample();
        f.setRootCauseAnalysis("Identity provider misconfigured during migration.");
        f.setOwner("alice");
        f.setDueDate(LocalDate.of(2026, 6, 30));
        f.setCreatedBy("bob");

        assertThat(f.getRootCauseAnalysis()).isEqualTo("Identity provider misconfigured during migration.");
        assertThat(f.getOwner()).isEqualTo("alice");
        assertThat(f.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(f.getCreatedBy()).isEqualTo("bob");
    }

    @Test
    void transitionStatusRejectsNullTarget() {
        var f = sample();
        assertThatThrownBy(() -> f.transitionStatus(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void invalidTransitionExceptionCarriesDetailMap() {
        var f = sample();
        assertThatThrownBy(() -> f.transitionStatus(FindingStatus.VERIFIED_CLOSED))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(thrown -> {
                    var ex = (DomainValidationException) thrown;
                    assertThat(ex.getDetail()).containsEntry("current_status", "OPEN");
                    assertThat(ex.getDetail()).containsEntry("target_status", "VERIFIED_CLOSED");
                    assertThat(ex.getDetail()).containsKey("valid_targets");
                });
    }

    @Test
    void transitionStatusAdvancesThroughLifecycle() {
        var f = sample();
        f.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        f.transitionStatus(FindingStatus.REMEDIATION_COMPLETE);
        f.transitionStatus(FindingStatus.VERIFIED_CLOSED);
        assertThat(f.getStatus()).isEqualTo(FindingStatus.VERIFIED_CLOSED);
    }

    @Test
    void remediationCompleteCanReopenToInProgress() {
        var f = sample();
        f.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        f.transitionStatus(FindingStatus.REMEDIATION_COMPLETE);
        f.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        assertThat(f.getStatus()).isEqualTo(FindingStatus.REMEDIATION_IN_PROGRESS);
    }

    @Test
    void verifiedClosedIsTerminal() {
        var f = sample();
        f.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        f.transitionStatus(FindingStatus.REMEDIATION_COMPLETE);
        f.transitionStatus(FindingStatus.VERIFIED_CLOSED);
        for (FindingStatus target : FindingStatus.values()) {
            assertThatThrownBy(() -> f.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
        }
    }

    private Finding sample() {
        return new Finding(
                PROJECT,
                "FIND-001",
                "MFA missing on admin portal",
                FindingType.CONTROL_DEFICIENCY,
                FindingSeverity.HIGH,
                "Admin portal accepts password-only auth.");
    }
}
