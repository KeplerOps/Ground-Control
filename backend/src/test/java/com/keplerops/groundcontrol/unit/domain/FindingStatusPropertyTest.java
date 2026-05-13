package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Tag;

/**
 * Property-based coverage of the Finding lifecycle DAG (L2 per ADR-012):
 * every (from, to) pair in FindingStatus × FindingStatus is walked through
 * canTransitionTo + the entity's transitionStatus.
 */
@Tag("slow")
class FindingStatusPropertyTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        com.keplerops.groundcontrol.TestUtil.setField(
                project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return project;
    }

    @Provide
    Arbitrary<FindingStatus> statuses() {
        return Arbitraries.of(FindingStatus.values());
    }

    @Property
    void canTransitionToIsConsistentWithValidTargets(
            @ForAll("statuses") FindingStatus source, @ForAll("statuses") FindingStatus target) {
        assertThat(source.canTransitionTo(target))
                .isEqualTo(source.validTargets().contains(target));
    }

    @Property
    void validTransitionAlwaysChangesStatus(
            @ForAll("statuses") FindingStatus source, @ForAll("statuses") FindingStatus target) {
        Assume.that(source.canTransitionTo(target));

        var finding = newFinding();
        walkToStatus(finding, source);

        finding.transitionStatus(target);
        assertThat(finding.getStatus()).isEqualTo(target);
    }

    @Property
    void invalidTransitionAlwaysThrows(
            @ForAll("statuses") FindingStatus source, @ForAll("statuses") FindingStatus target) {
        Assume.that(!source.canTransitionTo(target));

        var finding = newFinding();
        walkToStatus(finding, source);

        assertThatThrownBy(() -> finding.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
    }

    @Property
    void invalidTransitionLeavesStatusUnchanged(
            @ForAll("statuses") FindingStatus source, @ForAll("statuses") FindingStatus target) {
        Assume.that(!source.canTransitionTo(target));

        var finding = newFinding();
        walkToStatus(finding, source);

        try {
            finding.transitionStatus(target);
        } catch (DomainValidationException expected) {
            // expected — see invalidTransitionAlwaysThrows
        }
        assertThat(finding.getStatus()).isEqualTo(source);
    }

    private Finding newFinding() {
        return new Finding(
                TEST_PROJECT,
                "FIND-PROP",
                "Property test finding",
                FindingType.AUDIT_FINDING,
                FindingSeverity.HIGH,
                "Test description");
    }

    /**
     * Walk a fresh finding (starts in OPEN) to the desired status by following
     * the valid transition path declared on {@link FindingStatus}.
     */
    private void walkToStatus(Finding finding, FindingStatus desired) {
        if (desired == FindingStatus.OPEN) return;
        finding.transitionStatus(FindingStatus.REMEDIATION_IN_PROGRESS);
        if (desired == FindingStatus.REMEDIATION_IN_PROGRESS) return;
        finding.transitionStatus(FindingStatus.REMEDIATION_COMPLETE);
        if (desired == FindingStatus.REMEDIATION_COMPLETE) return;
        finding.transitionStatus(FindingStatus.VERIFIED_CLOSED);
    }
}
