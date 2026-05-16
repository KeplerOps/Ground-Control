package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;

/**
 * Partial-update payload for {@link com.keplerops.groundcontrol.domain.assets.model.OperationalAsset}.
 *
 * <p>{@code name} / {@code description} / {@code assetType} retain the existing
 * null-means-unchanged semantics. The GC-M012 nullable metadata fields
 * ({@code owner}, {@code steward}, {@code environment}, {@code criticality},
 * {@code businessContext}, {@code scopeDesignation}) also use null-means-unchanged
 * for assignment, plus a paired {@code clear*} boolean that lets callers reset a
 * previously-designated value back to NULL ("not designated"). This mirrors the
 * {@code clearRootCauseAnalysis}/{@code clearOwner}/{@code clearDueDate} pattern
 * on {@code UpdateFindingCommand}; the clear flag takes precedence so callers
 * can both clear and assign in the same payload without ambiguity (the assign
 * wins). See ADR-038 + the GC-M012 plan for the rationale.
 */
public record UpdateAssetCommand(
        String name,
        String description,
        AssetType assetType,
        String owner,
        String steward,
        AssetEnvironment environment,
        AssetCriticality criticality,
        String businessContext,
        AssetScope scopeDesignation,
        boolean clearOwner,
        boolean clearSteward,
        boolean clearEnvironment,
        boolean clearCriticality,
        boolean clearBusinessContext,
        boolean clearScopeDesignation) {

    public UpdateAssetCommand(String name, String description, AssetType assetType) {
        this(
                name,
                description,
                assetType,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    public UpdateAssetCommand(
            String name,
            String description,
            AssetType assetType,
            String owner,
            String steward,
            AssetEnvironment environment,
            AssetCriticality criticality,
            String businessContext,
            AssetScope scopeDesignation) {
        this(
                name,
                description,
                assetType,
                owner,
                steward,
                environment,
                criticality,
                businessContext,
                scopeDesignation,
                false,
                false,
                false,
                false,
                false,
                false);
    }
}
