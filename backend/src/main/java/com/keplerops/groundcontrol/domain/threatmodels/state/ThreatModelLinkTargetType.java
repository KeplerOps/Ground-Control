package com.keplerops.groundcontrol.domain.threatmodels.state;

/**
 * Target types for a threat-model link. Internal first-class targets are
 * resolved via {@code GraphTargetResolverService#validateThreatModelTarget}
 * against project-scoped repositories. External targets stay as
 * {@code targetIdentifier} strings until they become first-class entities.
 */
public enum ThreatModelLinkTargetType {
    ASSET,
    REQUIREMENT,
    CONTROL,
    RISK_SCENARIO,
    OBSERVATION,
    RISK_ASSESSMENT_RESULT,
    VERIFICATION_RESULT,
    ARCHITECTURE_MODEL,
    CODE,
    ISSUE,
    EVIDENCE,
    EXTERNAL
}
