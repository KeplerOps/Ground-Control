package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import jakarta.validation.constraints.Size;

/**
 * Partial update for a threat model. Use {@code clearStride} / {@code clearNarrative}
 * to explicitly null an optional field — passing {@code null} in {@code stride} /
 * {@code narrative} alone is treated as "no change". Required fields ({@code title},
 * {@code threatSource}, {@code threatEvent}, {@code effect}) reject blank strings
 * server-side when present.
 */
public record UpdateThreatModelRequest(
        @Size(max = 200) String title,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative,
        Boolean clearStride,
        Boolean clearNarrative) {}
