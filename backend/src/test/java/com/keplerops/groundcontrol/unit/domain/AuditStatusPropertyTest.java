package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
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
 * Property-based coverage of the Audit lifecycle DAG (L2 per ADR-012):
 * every (from, to) pair in AuditStatus × AuditStatus is walked through
 * canTransitionTo + the entity's transitionStatus.
 */
@Tag("slow")
class AuditStatusPropertyTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        com.keplerops.groundcontrol.TestUtil.setField(
                project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return project;
    }

    @Provide
    Arbitrary<AuditStatus> statuses() {
        return Arbitraries.of(AuditStatus.values());
    }

    @Property
    void canTransitionToIsConsistentWithValidTargets(
            @ForAll("statuses") AuditStatus source, @ForAll("statuses") AuditStatus target) {
        assertThat(source.canTransitionTo(target))
                .isEqualTo(source.validTargets().contains(target));
    }

    @Property
    void validTransitionAlwaysChangesStatus(
            @ForAll("statuses") AuditStatus source, @ForAll("statuses") AuditStatus target) {
        Assume.that(source.canTransitionTo(target));

        var audit = newAudit();
        walkToStatus(audit, source);

        audit.transitionStatus(target);
        assertThat(audit.getStatus()).isEqualTo(target);
    }

    @Property
    void invalidTransitionAlwaysThrows(@ForAll("statuses") AuditStatus source, @ForAll("statuses") AuditStatus target) {
        Assume.that(!source.canTransitionTo(target));

        var audit = newAudit();
        walkToStatus(audit, source);

        assertThatThrownBy(() -> audit.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
    }

    @Property
    void invalidTransitionLeavesStatusUnchanged(
            @ForAll("statuses") AuditStatus source, @ForAll("statuses") AuditStatus target) {
        Assume.that(!source.canTransitionTo(target));

        var audit = newAudit();
        walkToStatus(audit, source);

        try {
            audit.transitionStatus(target);
        } catch (DomainValidationException expected) {
            // expected — see invalidTransitionAlwaysThrows
        }
        assertThat(audit.getStatus()).isEqualTo(source);
    }

    private Audit newAudit() {
        return new Audit(TEST_PROJECT, "AUDIT-PROP", "Property test audit", AuditType.INTERNAL, "Test scope.");
    }

    /**
     * Walk a fresh audit (starts in PLANNED) to the desired status by following
     * the valid transition path declared on {@link AuditStatus}.
     */
    private void walkToStatus(Audit audit, AuditStatus desired) {
        if (desired == AuditStatus.PLANNED) return;
        audit.transitionStatus(AuditStatus.IN_PROGRESS);
        if (desired == AuditStatus.IN_PROGRESS) return;
        audit.transitionStatus(AuditStatus.DRAFT_REPORT);
        if (desired == AuditStatus.DRAFT_REPORT) return;
        audit.transitionStatus(AuditStatus.FINAL_REPORT);
        if (desired == AuditStatus.FINAL_REPORT) return;
        audit.transitionStatus(AuditStatus.CLOSED);
    }
}
