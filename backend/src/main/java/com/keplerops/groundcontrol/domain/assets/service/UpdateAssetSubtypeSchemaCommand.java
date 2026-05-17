package com.keplerops.groundcontrol.domain.assets.service;

import java.util.Map;

/**
 * Partial-update payload for {@link com.keplerops.groundcontrol.domain.assets.model.AssetSubtypeSchema}.
 *
 * <p>Each field uses null-means-unchanged semantics. The paired {@code clear*}
 * boolean lets callers reset a previously-set value back to NULL; the clear
 * flag wins over assign so the two can ride in the same payload without
 * ambiguity, matching the {@link UpdateAssetCommand} convention.
 *
 * <p>{@code schemaBody} replacement is atomic — passing a non-null map replaces
 * the entire schema body. Partial-field merges are intentionally not supported;
 * a new schema version should be registered instead.
 */
public record UpdateAssetSubtypeSchemaCommand(
        String description, Map<String, Object> schemaBody, boolean clearDescription, boolean clearSchemaBody) {}
