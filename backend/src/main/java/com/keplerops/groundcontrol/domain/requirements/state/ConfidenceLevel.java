package com.keplerops.groundcontrol.domain.requirements.state;

/**
 * Confidence band for derived analysis evidence (currently status-drift findings).
 *
 * <p>Ordered weakest to strongest so {@code ordinal()} carries comparison meaning:
 * {@code LOW < MEDIUM < HIGH}.
 */
public enum ConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH;

    /** True if this band is at least as strong as {@code other}. */
    public boolean atLeast(ConfidenceLevel other) {
        return this.compareTo(other) >= 0;
    }

    /** The stronger of the two bands ({@code this} on a tie). */
    public ConfidenceLevel strongest(ConfidenceLevel other) {
        return this.compareTo(other) >= 0 ? this : other;
    }
}
