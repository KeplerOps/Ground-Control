package com.keplerops.groundcontrol.api.admin.users;

import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/v1/admin/users/{username}/enabled}. Boxed {@code Boolean} so the
 * absence of the field surfaces as a {@code validation_error} (the default for primitives is
 * {@code false}, which would silently disable users).
 */
public record UpdateUserEnabledRequest(@NotNull Boolean enabled) {}
