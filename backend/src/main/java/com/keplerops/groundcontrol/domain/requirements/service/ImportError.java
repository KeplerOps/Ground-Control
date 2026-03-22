package com.keplerops.groundcontrol.domain.requirements.service;

/**
 * Details of a single error that occurred during a requirements import.
 *
 * @param phase import phase in which the error occurred: {@code "requirements"},
 *     {@code "relations"}, or {@code "traceability"}
 * @param uid UID of the requirement involved (source UID for relation errors)
 * @param error human-readable description of the failure
 * @param parent UID of the parent requirement involved, or null if not applicable
 * @param target UID of the target requirement involved (for explicit relation errors), or null
 * @param issueRef GitHub issue reference involved (for traceability errors), or null
 */
public record ImportError(
        String phase, String uid, String error, String parent, String target, String issueRef) {}
