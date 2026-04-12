package com.keplerops.groundcontrol.domain.threatmodels.state;

/**
 * STRIDE threat-modeling taxonomy categories. Optional on threat model entries;
 * other taxonomies (LINDDUN, PASTA, custom) may be carried in the free-text
 * narrative field instead per ADR-024.
 */
public enum StrideCategory {
    SPOOFING,
    TAMPERING,
    REPUDIATION,
    INFORMATION_DISCLOSURE,
    DENIAL_OF_SERVICE,
    ELEVATION_OF_PRIVILEGE
}
