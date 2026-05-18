package com.keplerops.groundcontrol.domain.grcanalysis.service;

/**
 * Projection mode for {@link ObservationProjectionService}.
 *
 * <p>{@code ASSET_EXPOSURE} projects current-state observations per active asset
 * within the project (filtered to one asset when {@code assetId} is supplied).
 * {@code CONTROL_STATE} cross-references the latest
 * {@link com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment}
 * for a control, never inferring effectiveness from
 * {@link com.keplerops.groundcontrol.domain.controls.state.ControlStatus#OPERATIONAL}
 * (preflight anti-pattern, see {@code architecture/notes/mcp-grc-analysis-tools-preflight.md}).
 *
 * <p>Public enum — kept all-caps and simple per ADR-034. MCP/frontend mirroring
 * is the parent task's responsibility.
 */
public enum ObservationProjectionMode {
    ASSET_EXPOSURE,
    CONTROL_STATE
}
