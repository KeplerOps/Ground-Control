package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;

public record TrustDecision(TrustOutcome outcome, String reason, String policyId) {}
