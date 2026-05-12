package com.keplerops.groundcontrol.api.admin.users;

import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;

/**
 * Response shape for {@code /api/v1/admin/users} endpoints. Carries the principal-only fields
 * that ADR-037 §4 allows: username, role, enabled. No password material or hash is ever
 * surfaced through this envelope.
 */
public record UserResponse(String username, Role role, boolean enabled) {}
