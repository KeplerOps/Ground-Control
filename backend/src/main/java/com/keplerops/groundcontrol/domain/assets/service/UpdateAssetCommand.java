package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import java.util.Map;

/**
 * Partial-update payload for {@link com.keplerops.groundcontrol.domain.assets.model.OperationalAsset}.
 *
 * <p>{@code name} / {@code description} / {@code assetType} retain the existing
 * null-means-unchanged semantics. The GC-M012 nullable metadata fields
 * ({@code owner}, {@code steward}, {@code environment}, {@code criticality},
 * {@code businessContext}, {@code scopeDesignation}) and the GC-M011 nullable
 * fields ({@code subtype}, {@code metadata}) also use null-means-unchanged for
 * assignment, plus a paired {@code clear*} boolean that lets callers reset a
 * previously-set value back to NULL. The clear flag takes precedence so callers
 * can both clear and assign in the same payload without ambiguity (the clear
 * wins). See ADR-038 + the GC-M012 / GC-M011 plans for the rationale.
 *
 * <p>{@code metadata} replacement is atomic — a non-null map replaces the
 * entire map. Partial-key merges are intentionally not supported; callers
 * compose the new map client-side.
 *
 * <p>GC-M018: {@code knowledgeState} uses null-means-unchanged. The
 * underlying column is NOT NULL so there is no clear flag — to revert to
 * the default a caller assigns {@link KnowledgeState#CONFIRMED} explicitly.
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
        String subtype,
        Map<String, Object> metadata,
        KnowledgeState knowledgeState,
        boolean clearOwner,
        boolean clearSteward,
        boolean clearEnvironment,
        boolean clearCriticality,
        boolean clearBusinessContext,
        boolean clearScopeDesignation,
        boolean clearSubtype,
        boolean clearMetadata) {

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
                null,
                null,
                null,
                false,
                false,
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
                null,
                null,
                null,
                false,
                false,
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
            AssetScope scopeDesignation,
            boolean clearOwner,
            boolean clearSteward,
            boolean clearEnvironment,
            boolean clearCriticality,
            boolean clearBusinessContext,
            boolean clearScopeDesignation) {
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
                null,
                null,
                null,
                clearOwner,
                clearSteward,
                clearEnvironment,
                clearCriticality,
                clearBusinessContext,
                clearScopeDesignation,
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
            AssetScope scopeDesignation,
            String subtype,
            Map<String, Object> metadata,
            boolean clearOwner,
            boolean clearSteward,
            boolean clearEnvironment,
            boolean clearCriticality,
            boolean clearBusinessContext,
            boolean clearScopeDesignation,
            boolean clearSubtype,
            boolean clearMetadata) {
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
                subtype,
                metadata,
                null,
                clearOwner,
                clearSteward,
                clearEnvironment,
                clearCriticality,
                clearBusinessContext,
                clearScopeDesignation,
                clearSubtype,
                clearMetadata);
    }
}
