package com.keplerops.groundcontrol.shared.security;

import java.util.regex.Pattern;

/**
 * Single Java source of truth for the ADR-037 user-credential validation contract — username
 * pattern, password length range. Used by:
 *
 * <ul>
 *   <li>{@code CreateUserRequest} / {@code UserAdminController} path validation
 *       (Bean-Validation {@code @Pattern} / {@code @Size}).
 *   <li>{@code UserAdminService} (post-binding programmatic validation called from non-web
 *       callers such as the bootstrap runner).
 *   <li>{@code FirstAdminBootstrapRunner} (CLI input validation).
 * </ul>
 *
 * <p>The V059 Flyway migration's {@code CHECK} constraint mirrors {@link #USERNAME_REGEX} as the
 * database-side backstop; the constants here and the SQL CHECK are kept in lockstep by
 * {@code UserCredentialPolicyContractTest}, which extracts the regex from both sides and
 * asserts equivalence. Add a new constraint here only after updating the SQL CHECK and the
 * docs at {@code docs/API.md#admin-users-adr-036}.
 */
public final class UserCredentialPolicy {

    /**
     * Username regex: lowercase ASCII identifier, 2-64 chars, starts with a letter,
     * subsequent chars are letters / digits / dot / underscore / hyphen. Same expression used
     * in the V059 {@code CHECK (username ~ '^[a-z][a-z0-9._-]{1,63}$')}.
     */
    public static final String USERNAME_REGEX = "^[a-z][a-z0-9._-]{1,63}$";

    /** Compiled form for programmatic callers (service, bootstrap). */
    public static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    /** Lower password length bound (ADR-037 §5: hashes must be resistant to offline cracking). */
    public static final int MIN_PASSWORD_LENGTH = 12;

    /** Upper bound: keeps BCrypt within its 72-byte cost ceiling and the storage column width. */
    public static final int MAX_PASSWORD_LENGTH = 200;

    private UserCredentialPolicy() {
        // constants only
    }
}
