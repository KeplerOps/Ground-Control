package com.keplerops.groundcontrol.domain.threatmodels.state;

/**
 * Semantic relationships between a threat model entry and its linked target.
 */
public enum ThreatModelLinkType {
    AFFECTS,
    EXPLOITS,
    MITIGATED_BY,
    ASSESSED_IN,
    OBSERVED_IN,
    DOCUMENTED_IN,
    ASSOCIATED
}
