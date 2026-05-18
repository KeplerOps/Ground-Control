package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Partial-update payload for {@code PUT /api/v1/assets/{id}}. Null = leave
 * field unchanged. To reset a previously-designated GC-M012 metadata field or
 * a GC-M011 subtype/metadata field back to NULL, set the paired {@code clear*}
 * boolean to true (e.g. {@code "clearCriticality": true},
 * {@code "clearSubtype": true}). The clear flag wins over a non-null
 * assignment in the same payload, so callers can both clear and re-designate
 * in a single PUT without ambiguity — the assign loses.
 *
 * <p>GC-M018 {@code knowledgeState} has no clear flag: the underlying column
 * is NOT NULL. Null on update = leave unchanged.
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
        @Size(max = 100) String subtype,
        Map<String, Object> metadata,
        KnowledgeState knowledgeState,
        boolean clearOwner,
        boolean clearSteward,
        boolean clearEnvironment,
        boolean clearCriticality,
        boolean clearBusinessContext,
        boolean clearScopeDesignation,
        boolean clearSubtype,
        boolean clearMetadata) {}
