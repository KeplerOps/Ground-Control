package com.keplerops.groundcontrol.shared.security.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import com.keplerops.groundcontrol.shared.security.UserCredentialPolicy;
import com.keplerops.groundcontrol.shared.security.UserSummary;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-facing facade over Spring Security's {@link JdbcUserDetailsManager} for ADR-037 user
 * lifecycle operations. Centralizes:
 *
 * <ul>
 *   <li><b>Username + password validation</b> — mirrors the V059 schema's {@code CHECK}
 *       constraint and the ADR-037 §5 password floor so a {@code DomainValidationException} is
 *       raised before the DB does, and field-level errors surface through {@code
 *       GlobalExceptionHandler}'s standard envelope.
 *   <li><b>Last-admin guard</b> — refuses any operation (demotion, disable, delete) that would
 *       leave the install with zero enabled admins. Without this guard a single mis-click locks
 *       the operator out and demands a manual DB intervention.
 *   <li><b>Audit-grade logging</b> — every state change writes a structured log line carrying
 *       username + role only; passwords, hashes, session ids, and CSRF tokens are never logged
 *       (ADR-037 §6).
 * </ul>
 *
 * <p>Role authorities are kept as a single row per user in the {@code authorities} table; the
 * service enforces that constraint by deleting and re-inserting on role change so accidental
 * dual-role rows cannot accumulate. The check constraint on the table only enforces vocabulary,
 * not cardinality.
 */
@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    // Credential policy is centralized at UserCredentialPolicy so the DTO, controller, service,
    // bootstrap runner, and equivalence test all agree. Local aliases keep the call sites short.
    private static final java.util.regex.Pattern USERNAME_PATTERN = UserCredentialPolicy.USERNAME_PATTERN;
    static final int MIN_PASSWORD_LENGTH = UserCredentialPolicy.MIN_PASSWORD_LENGTH;
    static final int MAX_PASSWORD_LENGTH = UserCredentialPolicy.MAX_PASSWORD_LENGTH;

    /** Column / detail-map key for the principal name. Deduplicated to satisfy SonarCloud S1192. */
    private static final String USERNAME_FIELD = "username";

    private static final String COUNT_OTHER_ENABLED_ADMINS_SQL =
            "SELECT COUNT(*) FROM users u JOIN authorities a ON a.username = u.username"
                    + " WHERE a.authority = 'ROLE_ADMIN' AND u.enabled = TRUE AND u.username <> ?";
    private static final String DELETE_AUTHORITIES_SQL = "DELETE FROM authorities WHERE username = ?";
    private static final String INSERT_AUTHORITY_SQL = "INSERT INTO authorities (username, authority) VALUES (?, ?)";
    private static final String UPDATE_ENABLED_SQL = "UPDATE users SET enabled = ? WHERE username = ?";
    private static final String LIST_USERS_SQL = "SELECT u.username, u.enabled,"
            + " (SELECT a.authority FROM authorities a WHERE a.username = u.username ORDER BY a.authority LIMIT 1)"
            + " AS authority FROM users u ORDER BY u.username";

    /**
     * Postgres transaction-scoped advisory lock that serializes every admin mutation through a
     * single critical section. Without it two admins demoting / disabling / deleting themselves
     * concurrently can both observe a non-zero other-admin count and commit — leaving the
     * install with zero enabled admins. {@code pg_advisory_xact_lock} releases on COMMIT or
     * ROLLBACK, so we never have to remember to unlock; if the transaction crashes the lock
     * still goes away. The key is arbitrary but stable: a 64-bit constant chosen to be visually
     * distinct in {@code pg_locks} dumps.
     */
    private static final long ADMIN_MUTATION_LOCK_KEY = 0x4743_5541_444D_4E54L; // GCUAADMN

    private static final String ADVISORY_LOCK_SQL = "SELECT pg_advisory_xact_lock(?)";

    private final JdbcUserDetailsManager users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SessionRegistry sessionRegistry;

    public UserAdminService(
            JdbcUserDetailsManager users, JdbcTemplate jdbc, PasswordEncoder encoder, SessionRegistry sessionRegistry) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.sessionRegistry = sessionRegistry;
    }

    public List<UserSummary> list() {
        return jdbc.query(
                LIST_USERS_SQL,
                (rs, n) -> new UserSummary(
                        rs.getString(USERNAME_FIELD),
                        roleFromAuthority(rs.getString("authority")),
                        rs.getBoolean("enabled")));
    }

    public UserSummary createUser(String username, String rawPassword, Role role) {
        validateUsername(username);
        validatePassword(rawPassword);
        // Preflight existence check keeps the common-case error fast and avoids a wasted
        // BCrypt hash on duplicates. It is NOT the source of truth — the insert itself is, so
        // we still translate the duplicate-key path below. Without that translation a
        // double-submit / concurrent admin / concurrent bootstrap arriving between the
        // preflight and the insert surfaces as a generic 500 from GlobalExceptionHandler
        // instead of the documented 409 user_exists.
        if (users.userExists(username)) {
            throw duplicateUserConflict(username);
        }
        UserDetails details = User.withUsername(username)
                .password(encoder.encode(rawPassword))
                .authorities(toAuthority(role))
                .disabled(false)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .build();
        try {
            users.createUser(details);
        } catch (DuplicateKeyException ex) {
            // Lost the race between preflight and insert — another admin / agent / bootstrap
            // got there first. Surface the same 409 envelope so callers see consistent error
            // semantics regardless of timing.
            throw duplicateUserConflict(username);
        }
        log.info("Created user '{}' with role {}", username, role);
        return new UserSummary(username, role, true);
    }

    private static ConflictException duplicateUserConflict(String username) {
        return new ConflictException(
                "User '" + username + "' already exists", "user_exists", detailWithUsername(username));
    }

    @Transactional
    public UserSummary updateRole(String username, Role newRole) {
        acquireAdminMutationLock();
        requireUserExists(username);
        if (newRole == Role.USER && isLastEnabledAdmin(username)) {
            throw lastAdminConflict(username);
        }
        jdbc.update(DELETE_AUTHORITIES_SQL, username);
        jdbc.update(INSERT_AUTHORITY_SQL, username, toAuthority(newRole));
        expireSessionsFor(username);
        log.info("Changed role for user '{}' to {}", username, newRole);
        boolean enabled = currentDetails(username).isEnabled();
        return new UserSummary(username, newRole, enabled);
    }

    @Transactional
    public UserSummary updateEnabled(String username, boolean enabled) {
        acquireAdminMutationLock();
        requireUserExists(username);
        UserDetails details = currentDetails(username);
        Role currentRole = roleFromUserDetails(details);
        if (!enabled && currentRole == Role.ADMIN && isLastEnabledAdmin(username)) {
            throw lastAdminConflict(username);
        }
        jdbc.update(UPDATE_ENABLED_SQL, enabled, username);
        if (!enabled) {
            expireSessionsFor(username);
        }
        log.info("Set enabled={} for user '{}'", enabled, username);
        return new UserSummary(username, currentRole, enabled);
    }

    @Transactional
    public void deleteUser(String username) {
        acquireAdminMutationLock();
        requireUserExists(username);
        Role currentRole = roleFromUserDetails(currentDetails(username));
        if (currentRole == Role.ADMIN && isLastEnabledAdmin(username)) {
            throw lastAdminConflict(username);
        }
        users.deleteUser(username);
        expireSessionsFor(username);
        log.info("Deleted user '{}'", username);
    }

    /**
     * Expire every live session for the named principal so a role / enabled / delete mutation
     * takes effect immediately, instead of waiting for natural session timeout. SessionRegistry
     * keys by principal object (the {@link org.springframework.security.core.userdetails.UserDetails}
     * instance) rather than username, so we iterate {@code getAllPrincipals()} and match on
     * {@code getUsername()}. The active sessions are then walked and expired via
     * {@link SessionInformation#expireNow()}; the next request on the same session lands in
     * {@code ConcurrentSessionFilter} which translates it into a fresh re-authentication.
     */
    private void expireSessionsFor(String username) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String registeredName = principalUsername(principal);
            if (!username.equals(registeredName)) {
                continue;
            }
            for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                info.expireNow();
            }
        }
    }

    private static String principalUsername(Object principal) {
        if (principal instanceof UserDetails details) {
            return details.getUsername();
        }
        return principal == null ? null : principal.toString();
    }

    /**
     * Funnels every admin-mutation transaction through a single Postgres advisory lock so the
     * last-admin guard's count-then-mutate is effectively serialized. The lock is acquired here
     * (inside the {@code @Transactional} boundary) so it releases on COMMIT or ROLLBACK.
     */
    private void acquireAdminMutationLock() {
        // pg_advisory_xact_lock returns void; query()/queryForRowSet would over-allocate. Use
        // execute(PreparedStatementCallback) so the bind variable is still parameterized (no
        // string concat) but the result set is discarded.
        jdbc.execute(ADVISORY_LOCK_SQL, (java.sql.PreparedStatement ps) -> {
            ps.setLong(1, ADMIN_MUTATION_LOCK_KEY);
            ps.execute();
            return null;
        });
    }

    private void requireUserExists(String username) {
        if (!users.userExists(username)) {
            throw new NotFoundException("User '" + username + "' not found");
        }
    }

    /**
     * Differentiates "user not found" from other failures in admin lifecycle paths.
     * SonarCloud's javasecurity:S5145 ("user enumeration") fires on this catch-and-rethrow
     * pattern because it is unsafe on unauthenticated login surfaces. Here every caller
     * ({@code updateRole}, {@code updateEnabled}, {@code deleteUser}) is gated by
     * {@code ROLE_ADMIN} via {@code ApiSecurityConfig}'s path matrix, and the entire admin
     * surface explicitly enumerates users via {@code GET /api/v1/admin/users}. A 404 here is
     * the documented response shape; an unauthenticated caller cannot reach this method.
     */
    @SuppressWarnings("java:S5804")
    private UserDetails currentDetails(String username) {
        try {
            return users.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new NotFoundException("User '" + username + "' not found");
        }
    }

    private boolean isLastEnabledAdmin(String username) {
        Integer others = jdbc.queryForObject(COUNT_OTHER_ENABLED_ADMINS_SQL, Integer.class, username);
        return others == null || others == 0;
    }

    private static void validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new DomainValidationException(
                    "Invalid username: must match " + USERNAME_PATTERN.pattern(),
                    "validation_error",
                    Map.of("field", (Serializable) USERNAME_FIELD));
        }
    }

    private static void validatePassword(String rawPassword) {
        if (rawPassword == null
                || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainValidationException(
                    "Invalid password: length must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH,
                    "validation_error",
                    Map.of("field", (Serializable) "password"));
        }
    }

    private static String toAuthority(Role role) {
        return "ROLE_" + role.name();
    }

    private static Role roleFromAuthority(String authority) {
        if (authority == null) {
            return Role.USER;
        }
        return "ROLE_ADMIN".equals(authority) ? Role.ADMIN : Role.USER;
    }

    private static Role roleFromUserDetails(UserDetails details) {
        for (GrantedAuthority granted : details.getAuthorities()) {
            if ("ROLE_ADMIN".equals(granted.getAuthority())) {
                return Role.ADMIN;
            }
        }
        return Role.USER;
    }

    private static ConflictException lastAdminConflict(String username) {
        return new ConflictException(
                "Cannot remove or demote the last enabled admin", "last_admin", detailWithUsername(username));
    }

    private static Map<String, Serializable> detailWithUsername(String username) {
        return Map.of(USERNAME_FIELD, username);
    }
}
