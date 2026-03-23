package com.keplerops.groundcontrol.domain.requirements.service;

/**
 * Details of a single failed requirement in a bulk status transition.
 *
 * @param id the UUID of the requirement that failed, as a string
 * @param uid the human-readable UID of the requirement, or null if the requirement was not found
 * @param error human-readable description of why the transition failed
 */
public record BulkFailureDetail(String id, String uid, String error) {}
