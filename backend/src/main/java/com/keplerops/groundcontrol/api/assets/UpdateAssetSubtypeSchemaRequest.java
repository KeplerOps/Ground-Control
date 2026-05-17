package com.keplerops.groundcontrol.api.assets;

import java.util.Map;

/**
 * Partial-update payload for {@code PUT /api/v1/assets/subtype-schemas/{id}}.
 * Null = leave unchanged; {@code clear*} resets to NULL. Schema body
 * replacement is atomic — pass the full new schema map.
 */
public record UpdateAssetSubtypeSchemaRequest(
        String description, Map<String, Object> schemaBody, boolean clearDescription, boolean clearSchemaBody) {}
