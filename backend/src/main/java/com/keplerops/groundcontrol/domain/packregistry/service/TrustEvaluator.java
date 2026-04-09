package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.repository.TrustPolicyRepository;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TrustEvaluator {

    private static final Logger log = LoggerFactory.getLogger(TrustEvaluator.class);
    private static final int MAX_PATTERN_LENGTH = 500;

    private final TrustPolicyRepository trustPolicyRepository;

    public TrustEvaluator(TrustPolicyRepository trustPolicyRepository) {
        this.trustPolicyRepository = trustPolicyRepository;
    }

    public TrustDecision evaluate(
            UUID projectId, ResolvedPack resolvedPack, PackIntegrityVerification integrityVerification) {
        var policies = trustPolicyRepository.findByProjectIdAndEnabledOrderByPriorityAsc(projectId, true);

        if (policies.isEmpty()) {
            log.info(
                    "trust_evaluation_no_policies: pack_id={}",
                    resolvedPack.entry().getPackId());
            return new TrustDecision(TrustOutcome.UNKNOWN, "No trust policies configured", null);
        }

        for (var policy : policies) {
            var decision = evaluatePolicy(policy, resolvedPack, integrityVerification);
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

    private TrustDecision evaluatePolicy(
            TrustPolicy policy, ResolvedPack resolvedPack, PackIntegrityVerification integrityVerification) {
        var rules = policy.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (var rule : rules) {
            if (evaluateRule(rule, resolvedPack, integrityVerification)) {
                var reason = String.format(
                        "Rule matched: field='%s', operator='%s', value='%s' in policy '%s'",
                        rule.field().wireValue(), rule.operator(), rule.value(), policy.getName());
                return new TrustDecision(rule.outcome(), reason, policy.getId().toString());
            }
        }

        return null;
    }

    private boolean evaluateRule(
            TrustPolicyRule rule, ResolvedPack resolvedPack, PackIntegrityVerification integrityVerification) {
        var actualValue = extractField(resolvedPack, integrityVerification, rule.field());
        if (actualValue == null) {
            return false;
        }

        return switch (rule.operator()) {
            case EQUALS -> actualValue.equals(rule.value());
            case NOT_EQUALS -> !actualValue.equals(rule.value());
            case CONTAINS -> actualValue.contains(rule.value());
            case MATCHES_PATTERN -> {
                if (rule.value().length() > MAX_PATTERN_LENGTH) {
                    log.warn(
                            "trust_rule_pattern_too_long: length={}",
                            rule.value().length());
                    yield false;
                }
                try {
                    yield Pattern.compile(rule.value()).matcher(actualValue).matches();
                } catch (java.util.regex.PatternSyntaxException e) {
                    log.warn("trust_rule_invalid_pattern: pattern={}", rule.value());
                    yield false;
                }
            }
            case IN_LIST -> {
                var items = List.of(rule.value().split(","));
                yield items.stream().map(String::trim).anyMatch(actualValue::equals);
            }
        };
    }

    private String extractField(
            ResolvedPack resolvedPack, PackIntegrityVerification integrityVerification, TrustPolicyField fieldName) {
        var entry = resolvedPack.entry();
        return switch (fieldName) {
            case PUBLISHER -> entry.getPublisher();
            case PACK_ID -> entry.getPackId();
            case PACK_TYPE -> entry.getPackType() != null ? entry.getPackType().name() : null;
            case VERSION -> entry.getVersion();
            case SOURCE_URL -> entry.getSourceUrl();
            case CHECKSUM -> entry.getChecksum();
            case VERIFIED_CHECKSUM -> integrityVerification != null ? integrityVerification.verifiedChecksum() : null;
            case CHECKSUM_VERIFIED -> integrityVerification != null
                    ? Boolean.toString(integrityVerification.checksumVerified())
                    : null;
            case SIGNATURE_VERIFIED -> integrityVerification != null
                    ? stringifyNullableBoolean(integrityVerification.signatureVerified())
                    : null;
        };
    }

    private String stringifyNullableBoolean(Boolean value) {
        return value != null ? value.toString() : null;
    }
}
