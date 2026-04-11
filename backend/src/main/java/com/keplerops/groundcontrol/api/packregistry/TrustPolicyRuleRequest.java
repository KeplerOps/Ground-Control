package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TrustPolicyRuleRequest(
        @NotNull TrustPolicyField field,
        @NotNull TrustPolicyRuleOperator operator,
        @NotBlank String value,
        @NotNull TrustOutcome outcome) {

    public TrustPolicyRule toDomain() {
        return new TrustPolicyRule(field, operator, value, outcome);
    }

    public static List<TrustPolicyRule> toDomainList(List<TrustPolicyRuleRequest> rules) {
        return rules != null
                ? rules.stream().map(TrustPolicyRuleRequest::toDomain).toList()
                : null;
    }
}
