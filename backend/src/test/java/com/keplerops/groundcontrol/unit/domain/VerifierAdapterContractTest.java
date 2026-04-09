package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.verification.service.VerificationOutcome;
import com.keplerops.groundcontrol.domain.verification.service.VerificationRequest;
import com.keplerops.groundcontrol.domain.verification.service.VerifierAdapter;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link VerifierAdapter}. Uses a minimal stub to verify the interface is
 * expressive enough for all tool categories listed in GC-F005: OpenJML, TLA+/TLC, OPA/Rego,
 * Frama-C, and manual review.
 */
class VerifierAdapterContractTest {

    private static final Instant NOW = Instant.parse("2026-04-08T12:00:00Z");

    /** Minimal stub that returns a configurable outcome. */
    static class StubAdapter implements VerifierAdapter {

        private final String prover;
        private final boolean available;
        private final VerificationOutcome fixedOutcome;

        StubAdapter(String prover, boolean available, VerificationOutcome fixedOutcome) {
            this.prover = prover;
            this.available = available;
            this.fixedOutcome = fixedOutcome;
        }

        @Override
        public String proverName() {
            return prover;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public VerificationOutcome verify(VerificationRequest request) {
            return fixedOutcome;
        }
    }

    @Nested
    class OpenJml {

        @Test
        void handlesEscVerification() {
            var outcome = new VerificationOutcome(
                    "openjml-esc",
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    "requires x > 0",
                    Map.of("solver", "z3", "duration_ms", 450),
                    NOW,
                    null);
            var adapter = new StubAdapter("openjml-esc", true, outcome);

            var request = new VerificationRequest(
                    "src/main/java/com/example/Foo.java", "requires x > 0", AssuranceLevel.L1, Map.of("mode", "esc"));

            var result = adapter.verify(request);

            assertThat(adapter.proverName()).isEqualTo("openjml-esc");
            assertThat(adapter.isAvailable()).isTrue();
            assertThat(result.prover()).isEqualTo("openjml-esc");
            assertThat(result.result()).isEqualTo(VerificationStatus.PROVEN);
            assertThat(result.assuranceLevel()).isEqualTo(AssuranceLevel.L1);
            assertThat(result.property()).isEqualTo("requires x > 0");
            assertThat(result.evidence()).containsEntry("solver", "z3");
        }
    }

    @Nested
    class TlaPlusTlc {

        @Test
        void handlesModelChecking() {
            var outcome = new VerificationOutcome(
                    "tlaplus-tlc",
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L2,
                    "TypeInvariant",
                    Map.of("states_explored", 42_000, "diameter", 15),
                    NOW,
                    NOW.plusSeconds(86400));
            var adapter = new StubAdapter("tlaplus-tlc", true, outcome);

            var request = new VerificationRequest(
                    "specs/tla/RequirementLifecycle.tla",
                    "TypeInvariant",
                    AssuranceLevel.L2,
                    Map.of("model", "MC_RequirementLifecycle.cfg"));

            var result = adapter.verify(request);

            assertThat(result.prover()).isEqualTo("tlaplus-tlc");
            assertThat(result.result()).isEqualTo(VerificationStatus.PROVEN);
            assertThat(result.assuranceLevel()).isEqualTo(AssuranceLevel.L2);
            assertThat(result.evidence()).containsEntry("states_explored", 42_000);
            assertThat(result.expiresAt()).isNotNull();
        }
    }

    @Nested
    class OpaRego {

        @Test
        void handlesPolicyVerification() {
            var outcome = new VerificationOutcome(
                    "opa",
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L1,
                    "allow_admin_access",
                    Map.of("decisions", 5, "bindings", Map.of("role", "admin")),
                    NOW,
                    null);
            var adapter = new StubAdapter("opa", true, outcome);

            var request = new VerificationRequest(
                    "policies/authz.rego",
                    "allow_admin_access",
                    AssuranceLevel.L1,
                    Map.of("input", Map.of("user", "alice", "role", "admin")));

            var result = adapter.verify(request);

            assertThat(result.prover()).isEqualTo("opa");
            assertThat(result.result()).isEqualTo(VerificationStatus.PROVEN);
            assertThat(result.evidence()).containsKey("decisions");
        }
    }

    @Nested
    class FramaC {

        @Test
        void handlesCSourceVerification() {
            var outcome = new VerificationOutcome(
                    "frama-c",
                    VerificationStatus.REFUTED,
                    AssuranceLevel.L3,
                    "assigns \\nothing",
                    Map.of("plugin", "wp", "counterexample", "buffer overflow at line 42"),
                    NOW,
                    null);
            var adapter = new StubAdapter("frama-c", true, outcome);

            var request = new VerificationRequest(
                    "src/crypto/aes.c", "assigns \\nothing", AssuranceLevel.L3, Map.of("plugin", "wp"));

            var result = adapter.verify(request);

            assertThat(result.prover()).isEqualTo("frama-c");
            assertThat(result.result()).isEqualTo(VerificationStatus.REFUTED);
            assertThat(result.assuranceLevel()).isEqualTo(AssuranceLevel.L3);
            assertThat(result.evidence()).containsEntry("plugin", "wp");
        }
    }

    @Nested
    class ManualReview {

        @Test
        void handlesManualReviewProcess() {
            var outcome = new VerificationOutcome(
                    "manual-review",
                    VerificationStatus.PROVEN,
                    AssuranceLevel.L0,
                    "Security review of auth module",
                    Map.of("reviewer", "jane.doe", "checklist_passed", true, "notes", "LGTM"),
                    NOW,
                    NOW.plusSeconds(7 * 86400));
            var adapter = new StubAdapter("manual-review", true, outcome);

            var request = new VerificationRequest(
                    "src/main/java/com/example/AuthService.java",
                    "Security review of auth module",
                    AssuranceLevel.L0,
                    Map.of("reviewer", "jane.doe", "checklist", "security-review-v2"));

            var result = adapter.verify(request);

            assertThat(result.prover()).isEqualTo("manual-review");
            assertThat(result.result()).isEqualTo(VerificationStatus.PROVEN);
            assertThat(result.evidence()).containsEntry("reviewer", "jane.doe");
            assertThat(result.expiresAt()).isNotNull();
        }
    }

    @Nested
    class Availability {

        @Test
        void unavailableAdapterReportsCorrectly() {
            var adapter = new StubAdapter("openjml-esc", false, null);

            assertThat(adapter.isAvailable()).isFalse();
            assertThat(adapter.proverName()).isEqualTo("openjml-esc");
        }
    }

    @Nested
    class OutcomeMapping {

        @Test
        void outcomeFieldsMapToCreateCommand() {
            var outcome = new VerificationOutcome(
                    "tlaplus-tlc",
                    VerificationStatus.TIMEOUT,
                    AssuranceLevel.L2,
                    "Liveness",
                    Map.of("timeout_seconds", 300),
                    NOW,
                    NOW.plusSeconds(3600));

            // Verify all fields needed by CreateVerificationResultCommand are present
            assertThat(outcome.prover()).isNotNull();
            assertThat(outcome.result()).isNotNull();
            assertThat(outcome.assuranceLevel()).isNotNull();
            assertThat(outcome.verifiedAt()).isNotNull();
            // Nullable fields
            assertThat(outcome.property()).isEqualTo("Liveness");
            assertThat(outcome.evidence()).isNotEmpty();
            assertThat(outcome.expiresAt()).isNotNull();
        }
    }
}
