package com.keplerops.groundcontrol.api.admin.users;

import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import com.keplerops.groundcontrol.shared.security.UserCredentialPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin-initiated user creation payload.
 *
 * <p>The username regex and password floor mirror the {@code V059__create_users.sql}
 * {@code CHECK} constraint and the policy floor in ADR-037 §5 so request validation fails fast
 * before any DB round-trip and so the SPA can surface field-level errors via the standard
 * {@code validation_error} envelope.
 */
public record CreateUserRequest(
        @NotBlank @Pattern(
                        regexp = UserCredentialPolicy.USERNAME_REGEX,
                        message =
                                "username must start with a lowercase letter and contain only lowercase letters, digits, '.', '_' or '-' (2-64 chars)")
                String username,
        @NotBlank @Size(
                        min = UserCredentialPolicy.MIN_PASSWORD_LENGTH,
                        max = UserCredentialPolicy.MAX_PASSWORD_LENGTH,
                        message = "password must be 12-200 characters")
                String password,
        @NotNull Role role) {}
