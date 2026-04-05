package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskAssessmentApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record RiskAssessmentApprovalStateTransitionRequest(@NotNull RiskAssessmentApprovalStatus approvalState) {}
