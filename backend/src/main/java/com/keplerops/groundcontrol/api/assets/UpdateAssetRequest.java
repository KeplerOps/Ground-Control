package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import jakarta.validation.constraints.Size;

/**
 * Partial-update payload for {@code PUT /api/v1/assets/{id}}. Null = leave
 * field unchanged. To reset a previously-designated GC-M012 metadata field
 * back to NULL ("not designated"), set the paired {@code clear*} boolean to
 * true (e.g. {@code "clearCriticality": true}). The clear flag wins over a
 * non-null assignment in the same payload, so callers can both clear and
 * re-designate in a single PUT without ambiguity — the assign loses.
 */
public record UpdateAssetRequest(
        @Size(max = 200) String name,
        String description,
        AssetType assetType,
        @Size(max = 200) String owner,
        @Size(max = 200) String steward,
        AssetEnvironment environment,
        AssetCriticality criticality,
        String businessContext,
        AssetScope scopeDesignation,
        boolean clearOwner,
        boolean clearSteward,
        boolean clearEnvironment,
        boolean clearCriticality,
        boolean clearBusinessContext,
        boolean clearScopeDesignation) {}
