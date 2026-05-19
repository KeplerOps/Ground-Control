package com.keplerops.groundcontrol.api.admin.users;

import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/v1/admin/users/{username}/role}. The username is in the path so
 * a single user cannot accidentally re-target someone else's role via a body field that drifts
 * from the URL.
 */
public record UpdateUserRoleRequest(@NotNull Role role) {}
