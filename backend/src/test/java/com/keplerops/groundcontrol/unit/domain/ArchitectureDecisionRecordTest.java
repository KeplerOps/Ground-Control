package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchitectureDecisionRecordTest {

    private static Project createProject() {
        return new Project("test-project", "Test Project");
    }

    private static ArchitectureDecisionRecord createAdr() {
        return new ArchitectureDecisionRecord(
                createProject(),
                "ADR-001",
                "Use PostgreSQL",
                LocalDate.of(2026, 3, 31),
                "We need a database",
                "Use PostgreSQL",
                "Proven, reliable",
                "test-user");
    }

    @Nested
    class Defaults {

        @Test
        void statusDefaultsToProposed() {
            var adr = createAdr();
            assertThat(adr.getStatus()).isEqualTo(AdrStatus.PROPOSED);
        }

        @Test
        void supersededByDefaultsToNull() {
            var adr = createAdr();
            assertThat(adr.getSupersededBy()).isNull();
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void proposedToAcceptedSucceeds() {
            var adr = createAdr();
            adr.transitionStatus(AdrStatus.ACCEPTED);
            assertThat(adr.getStatus()).isEqualTo(AdrStatus.ACCEPTED);
        }

        @Test
        void acceptedToDeprecatedSucceeds() {
            var adr = createAdr();
            adr.transitionStatus(AdrStatus.ACCEPTED);
            adr.transitionStatus(AdrStatus.DEPRECATED);
            assertThat(adr.getStatus()).isEqualTo(AdrStatus.DEPRECATED);
        }

        @Test
        void acceptedToSupersededSucceeds() {
            var adr = createAdr();
            adr.transitionStatus(AdrStatus.ACCEPTED);
            adr.transitionStatus(AdrStatus.SUPERSEDED);
            assertThat(adr.getStatus()).isEqualTo(AdrStatus.SUPERSEDED);
        }

        @Test
        void proposedToDeprecatedFails() {
            var adr = createAdr();
            assertThatThrownBy(() -> adr.transitionStatus(AdrStatus.DEPRECATED))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot transition from PROPOSED to DEPRECATED");
        }

        @Test
        void deprecatedToAnythingFails() {
            var adr = createAdr();
            adr.transitionStatus(AdrStatus.ACCEPTED);
            adr.transitionStatus(AdrStatus.DEPRECATED);
            assertThatThrownBy(() -> adr.transitionStatus(AdrStatus.ACCEPTED))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void nullTransitionFails() {
            var adr = createAdr();
            assertThatThrownBy(() -> adr.transitionStatus(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Target status must not be null");
        }
    }

    @Nested
    class Accessors {

        @Test
        void allFieldsSetByConstructor() {
            var adr = createAdr();
            assertThat(adr.getUid()).isEqualTo("ADR-001");
            assertThat(adr.getTitle()).isEqualTo("Use PostgreSQL");
            assertThat(adr.getDecisionDate()).isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(adr.getContext()).isEqualTo("We need a database");
            assertThat(adr.getDecision()).isEqualTo("Use PostgreSQL");
            assertThat(adr.getConsequences()).isEqualTo("Proven, reliable");
            assertThat(adr.getCreatedBy()).isEqualTo("test-user");
            assertThat(adr.getProject()).isNotNull();
        }

        @Test
        void settersUpdateFields() {
            var adr = createAdr();
            adr.setTitle("Updated title");
            adr.setDecisionDate(LocalDate.of(2026, 4, 1));
            adr.setContext("Updated context");
            adr.setDecision("Updated decision");
            adr.setConsequences("Updated consequences");
            adr.setSupersededBy("ADR-002");

            assertThat(adr.getTitle()).isEqualTo("Updated title");
            assertThat(adr.getDecisionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(adr.getContext()).isEqualTo("Updated context");
            assertThat(adr.getDecision()).isEqualTo("Updated decision");
            assertThat(adr.getConsequences()).isEqualTo("Updated consequences");
            assertThat(adr.getSupersededBy()).isEqualTo("ADR-002");
        }

        @Test
        void toStringReturnsUidAndTitle() {
            var adr = createAdr();
            assertThat(adr.toString()).isEqualTo("ADR-001: Use PostgreSQL");
        }
    }
}
