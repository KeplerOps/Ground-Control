package com.keplerops.groundcontrol.domain.packregistry.model;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyField;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustPolicyRuleOperator;

public record TrustPolicyRule(
        TrustPolicyField field, TrustPolicyRuleOperator operator, String value, TrustOutcome outcome) {}
