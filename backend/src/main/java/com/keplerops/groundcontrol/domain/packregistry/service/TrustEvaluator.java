package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.repository.TrustPolicyRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustEvaluator {

    private static final Logger log = LoggerFactory.getLogger(TrustEvaluator.class);

    private final TrustPolicyRepository trustPolicyRepository;

    public TrustEvaluator(TrustPolicyRepository trustPolicyRepository) {
        this.trustPolicyRepository = trustPolicyRepository;
    }

    public TrustDecision evaluate(UUID projectId, ResolvedPack resolvedPack) {
        var policies = trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(projectId, true);

        if (policies.isEmpty()) {
            log.info(
                    "trust_evaluation_no_policies: pack_id={}",
                    resolvedPack.entry().getPackId());
            return new TrustDecision(TrustOutcome.UNKNOWN, "No trust policies configured", null);
        }

        for (var policy : policies) {
            var decision = evaluatePolicy(policy, resolvedPack);
            if (decision != null) {
                log.info(
                        "trust_evaluation_decided: pack_id={}, outcome={}, policy={}",
                        resolvedPack.entry().getPackId(),
                        decision.outcome(),
                        policy.getName());
                return decision;
            }
        }

        // No rules matched in any policy; use the default of the highest-priority policy
        var firstPolicy = policies.getFirst();
        var defaultDecision = new TrustDecision(
                firstPolicy.getDefaultOutcome(),
                "No rules matched; using default outcome of policy '" + firstPolicy.getName() + "'",
                firstPolicy.getId().toString());
        log.info(
                "trust_evaluation_default: pack_id={}, outcome={}, policy={}",
                resolvedPack.entry().getPackId(),
                defaultDecision.outcome(),
                firstPolicy.getName());
        return defaultDecision;
    }

    private TrustDecision evaluatePolicy(TrustPolicy policy, ResolvedPack resolvedPack) {
        var rules = policy.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (var rule : rules) {
            if (evaluateRule(rule, resolvedPack)) {
                var outcomeStr = (String) rule.get("outcome");
                var outcome = TrustOutcome.valueOf(outcomeStr);
                var reason = String.format(
                        "Rule matched: field='%s', operator='%s', value='%s' in policy '%s'",
                        rule.get("field"), rule.get("operator"), rule.get("value"), policy.getName());
                return new TrustDecision(outcome, reason, policy.getId().toString());
            }
        }

        return null;
    }

    private boolean evaluateRule(Map<String, Object> rule, ResolvedPack resolvedPack) {
        var field = (String) rule.get("field");
        var operatorStr = (String) rule.get("operator");
        var ruleValue = (String) rule.get("value");

        if (field == null || operatorStr == null || ruleValue == null) {
            return false;
        }

        var actualValue = extractField(resolvedPack, field);
        if (actualValue == null) {
            return false;
        }

        var operator = TrustPolicyRuleOperator.valueOf(operatorStr);
        return switch (operator) {
            case EQUALS -> actualValue.equals(ruleValue);
            case NOT_EQUALS -> !actualValue.equals(ruleValue);
            case CONTAINS -> actualValue.contains(ruleValue);
            case MATCHES_PATTERN -> Pattern.matches(ruleValue, actualValue);
            case IN_LIST -> {
                var items = List.of(ruleValue.split(","));
                yield items.stream().map(String::trim).anyMatch(actualValue::equals);
            }
        };
    }

    private String extractField(ResolvedPack resolvedPack, String fieldName) {
        var entry = resolvedPack.entry();
        return switch (fieldName) {
            case "publisher" -> entry.getPublisher();
            case "packId" -> entry.getPackId();
            case "packType" -> entry.getPackType() != null ? entry.getPackType().name() : null;
            case "version" -> entry.getVersion();
            case "sourceUrl" -> entry.getSourceUrl();
            case "checksum" -> entry.getChecksum();
            default -> null;
        };
    }
}
