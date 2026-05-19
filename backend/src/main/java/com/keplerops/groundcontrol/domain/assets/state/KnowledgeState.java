package com.keplerops.groundcontrol.domain.assets.state;

/**
 * GC-M018: knowledge / completeness dimension for {@link
 * com.keplerops.groundcontrol.domain.assets.model.OperationalAsset} and
 * {@link com.keplerops.groundcontrol.domain.assets.model.AssetRelation}.
 *
 * <p>Distinct from {@link AssetType}, {@link AssetType#OTHER}, the
 * (subtype, metadata) bag, and the free-text {@code confidence} provenance
 * fields. Coverage gaps and unresolved dependencies are represented by
 * {@link #UNKNOWN}; provisional facts by {@link #PROVISIONAL}; asserted
 * facts by {@link #CONFIRMED}. Ordering is intentional: a CONFIRMED
 * assertion is "stronger" than a PROVISIONAL one, which is stronger than
 * an UNKNOWN one.
 */
public enum KnowledgeState {
    UNKNOWN(0),
    PROVISIONAL(1),
    CONFIRMED(2);

    private final int strength;

    KnowledgeState(int strength) {
        this.strength = strength;
    }

    /**
     * True when this state is at least as strong as {@code other}. Useful
     * for "show me everything CONFIRMED or better" filters consumed by
     * risk / threat / control workflows. Strength is declared explicitly
     * rather than read from {@link Enum#ordinal()} so a future reorder of
     * the declaration list cannot silently flip the ordering — errorprone
     * (EnumOrdinal) flags any ordinal-based comparison for the same
     * reason.
     */
    public boolean atLeast(KnowledgeState other) {
        return this.strength >= other.strength;
    }
}
