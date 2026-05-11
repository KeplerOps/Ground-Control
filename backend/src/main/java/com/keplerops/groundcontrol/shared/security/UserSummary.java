package com.keplerops.groundcontrol.shared.security;

import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;

/**
 * Internal view of a single Spring Security principal. Used by {@code UserAdminService} (and any
 * future non-web caller such as a CLI or scheduled task) so the service layer is not coupled to
 * the {@code api.admin.users.UserResponse} wire shape. The controller maps this to
 * {@code UserResponse} at the request boundary.
 */
public record UserSummary(String username, Role role, boolean enabled) {}
