package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.repository.TrustPolicyRepository;
import com.keplerops.groundcontrol.domain.packregistry.service.ResolvedPack;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustEvaluator;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustEvaluatorTest {

    @Mock
    private TrustPolicyRepository trustPolicyRepository;

    private TrustEvaluator evaluator;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private ResolvedPack makeResolvedPack(String publisher) {
        var project = makeProject();
        var entry = new PackRegistryEntry(project, "test-pack", PackType.CONTROL_PACK, "1.0.0");
        entry.setPublisher(publisher);
        entry.setSourceUrl("https://registry.example.com/test-pack");
        return new ResolvedPack(entry, "1.0.0", "https://registry.example.com/test-pack", "abc123", List.of());
    }

    @BeforeEach
    void setUp() {
        evaluator = new TrustEvaluator(trustPolicyRepository);
    }

    @Nested
    class Evaluate {

        @Test
        void returnsUnknownWhenNoPoliciesExist() {
            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of());

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.UNKNOWN);
            assertThat(decision.policyId()).isNull();
        }

        @Test
        void returnsTrustedWhenRuleMatches() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "allow-nist", TrustOutcome.REJECTED);
            setField(policy, "id", UUID.randomUUID());
            policy.setRules(
                    List.of(Map.of("field", "publisher", "operator", "EQUALS", "value", "NIST", "outcome", "TRUSTED")));
            policy.setPriority(1);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.TRUSTED);
        }

        @Test
        void returnsRejectedWhenRuleRejects() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "block-untrusted", TrustOutcome.TRUSTED);
            setField(policy, "id", UUID.randomUUID());
            policy.setRules(List.of(
                    Map.of("field", "publisher", "operator", "EQUALS", "value", "evil-corp", "outcome", "REJECTED")));
            policy.setPriority(1);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("evil-corp"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.REJECTED);
        }

        @Test
        void returnsDefaultOutcomeWhenNoRulesMatch() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "strict-policy", TrustOutcome.REJECTED);
            setField(policy, "id", UUID.randomUUID());
            policy.setRules(List.of(
                    Map.of("field", "publisher", "operator", "EQUALS", "value", "other-corp", "outcome", "TRUSTED")));
            policy.setPriority(1);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.REJECTED);
        }

        @Test
        void firstMatchingRuleWins() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "multi-rule", TrustOutcome.UNKNOWN);
            setField(policy, "id", UUID.randomUUID());
            policy.setRules(List.of(
                    Map.of("field", "publisher", "operator", "EQUALS", "value", "NIST", "outcome", "TRUSTED"),
                    Map.of("field", "publisher", "operator", "CONTAINS", "value", "NIS", "outcome", "REJECTED")));
            policy.setPriority(1);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.TRUSTED);
        }

        @Test
        void priorityOrderIsRespected() {
            var project = makeProject();

            var policy1 = new TrustPolicy(project, "reject-first", TrustOutcome.REJECTED);
            setField(policy1, "id", UUID.randomUUID());
            policy1.setRules(List.of(
                    Map.of("field", "publisher", "operator", "EQUALS", "value", "NIST", "outcome", "REJECTED")));
            policy1.setPriority(1);

            var policy2 = new TrustPolicy(project, "allow-second", TrustOutcome.TRUSTED);
            setField(policy2, "id", UUID.randomUUID());
            policy2.setRules(
                    List.of(Map.of("field", "publisher", "operator", "EQUALS", "value", "NIST", "outcome", "TRUSTED")));
            policy2.setPriority(2);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy1, policy2));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.REJECTED);
        }

        @Test
        void containsOperatorWorks() {
            var project = makeProject();
            var policy = new TrustPolicy(project, "contains-test", TrustOutcome.REJECTED);
            setField(policy, "id", UUID.randomUUID());
            policy.setRules(List.of(Map.of(
                    "field",
                    "sourceUrl",
                    "operator",
                    "CONTAINS",
                    "value",
                    "registry.example.com",
                    "outcome",
                    "TRUSTED")));
            policy.setPriority(1);

            when(trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(PROJECT_ID, true))
                    .thenReturn(List.of(policy));

            var decision = evaluator.evaluate(PROJECT_ID, makeResolvedPack("NIST"));
            assertThat(decision.outcome()).isEqualTo(TrustOutcome.TRUSTED);
        }
    }
}
