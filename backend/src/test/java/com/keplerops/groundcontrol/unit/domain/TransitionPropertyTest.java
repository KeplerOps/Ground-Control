package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.UUID;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

/**
 * Property-based tests for status transitions using jqwik.
 */
@Tag("slow")
class TransitionPropertyTest {

    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        com.keplerops.groundcontrol.TestUtil.setField(
                project, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return project;
    }

    @Provide
    Arbitrary<Status> statuses() {
        return Arbitraries.of(Status.values());
    }

    @Property
    void validTransitionAlwaysChangesStatus(@ForAll("statuses") Status source, @ForAll("statuses") Status target) {
        Assume.that(source.canTransitionTo(target));

        var req = new Requirement(TEST_PROJECT, "REQ-PROP", "Property test", "Statement");
        // Walk to source state
        walkToStatus(req, source);

        req.transitionStatus(target);
        assertThat(req.getStatus()).isEqualTo(target);
    }

    @Property
    void invalidTransitionAlwaysThrows(@ForAll("statuses") Status source, @ForAll("statuses") Status target) {
        Assume.that(!source.canTransitionTo(target));

        var req = new Requirement(TEST_PROJECT, "REQ-PROP", "Property test", "Statement");
        walkToStatus(req, source);

        assertThatThrownBy(() -> req.transitionStatus(target)).isInstanceOf(DomainValidationException.class);
    }

    @Property
    void canTransitionToIsConsistentWithValidTargets(
            @ForAll("statuses") Status source, @ForAll("statuses") Status target) {
        assertThat(source.canTransitionTo(target))
                .isEqualTo(source.validTargets().contains(target));
    }

    /**
     * Walk a fresh requirement (starts in DRAFT) to the desired status
     * by following the valid transition path.
     */
    private void walkToStatus(Requirement req, Status desired) {
        if (desired == Status.DRAFT) return;
        if (desired == Status.ACTIVE) {
            req.transitionStatus(Status.ACTIVE);
        } else if (desired == Status.DEPRECATED) {
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.DEPRECATED);
        } else if (desired == Status.ARCHIVED) {
            req.transitionStatus(Status.ACTIVE);
            req.transitionStatus(Status.ARCHIVED);
        }
    }
}
