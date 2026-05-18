package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateAssetRelationRequest(
        String description,
        @Size(max = 100) String sourceSystem,
        @Size(max = 500) String externalSourceId,
        Instant collectedAt,
        @Size(max = 50) String confidence,
        // GC-M018: null = leave unchanged. The underlying column is NOT
        // NULL, so there is no clear flag.
        KnowledgeState knowledgeState) {}
