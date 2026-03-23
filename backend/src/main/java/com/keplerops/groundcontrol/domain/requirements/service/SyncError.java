package com.keplerops.groundcontrol.domain.requirements.service;

/**
 * Details of a single error that occurred during a GitHub issue sync.
 *
 * @param phase sync phase in which the error occurred: {@code "upsert"} or
 *     {@code "traceability"}
 * @param issue GitHub issue number involved, or null if not applicable
 * @param artifactIdentifier artifact identifier of the traceability link involved, or null
 * @param error human-readable description of the failure
 */
public record SyncError(String phase, Integer issue, String artifactIdentifier, String error) {}
