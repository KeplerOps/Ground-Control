package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AssetRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String name,
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
        // GC-M018: knowledge / completeness state. Optional on create —
        // the service defaults to CONFIRMED when omitted, matching the
        // entity initializer and the DB default applied to legacy rows.
        KnowledgeState knowledgeState) {}
