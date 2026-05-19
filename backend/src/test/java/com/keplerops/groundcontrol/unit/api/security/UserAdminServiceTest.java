package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import com.keplerops.groundcontrol.shared.security.UserSummary;
import com.keplerops.groundcontrol.shared.security.service.UserAdminService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

/**
 * Unit coverage of the user-admin service. The mocked {@link JdbcUserDetailsManager} +
 * {@link JdbcTemplate} keep the boundary between Spring Security's contract and the local
 * last-admin guard / role mutation policy visible.
 *
 * <p>Tests in the {@code @Nested LastAdminGuard} class are the ones that *must* hold — the
 * guard is the only thing standing between an admin and an installation that can never recover
 * if they accidentally demote themselves.
 */
class UserAdminServiceTest {

    private JdbcUserDetailsManager users;
    private JdbcTemplate jdbc;
    private PasswordEncoder encoder;
    private SessionRegistry sessionRegistry;
    private UserAdminService service;

    @BeforeEach
    void setUp() {
        users = mock(JdbcUserDetailsManager.class);
        jdbc = mock(JdbcTemplate.class);
        encoder = new BCryptPasswordEncoder();
        sessionRegistry = mock(SessionRegistry.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());
        service = new UserAdminService(users, jdbc, encoder, sessionRegistry);
    }

    @Nested
    class CreateUser {

        @Test
        void hashesPasswordWithBcryptAndCreatesViaJdbcManager() {
            when(users.userExists("alice")).thenReturn(false);

            UserSummary response = service.createUser("alice", "correct-horse-battery-staple", Role.USER);

            assertThat(response.username()).isEqualTo("alice");
            assertThat(response.role()).isEqualTo(Role.USER);
            assertThat(response.enabled()).isTrue();
            verify(users)
                    .createUser(org.mockito.ArgumentMatchers.argThat(u -> "alice".equals(u.getUsername())
                            && encoder.matches("correct-horse-battery-staple", u.getPassword())
                            && u.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()))
                            && u.isEnabled()));
        }

        @Test
        void rejectsDuplicateUsernameWith409Conflict() {
            when(users.userExists("alice")).thenReturn(true);

            assertThatThrownBy(() -> service.createUser("alice", "long-enough-password", Role.USER))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("alice");
            verify(users, never()).createUser(any());
        }

        @Test
        void rejectsUsernameThatDoesNotMatchPattern() {
            assertThatThrownBy(() -> service.createUser("Bad Name!", "long-enough-password", Role.USER))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("username");
            verify(users, never()).createUser(any());
        }

        @Test
        void rejectsShortPassword() {
            assertThatThrownBy(() -> service.createUser("alice", "tooshort", Role.USER))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("password");
            verify(users, never()).createUser(any());
        }
    }

    @Nested
    class UpdateRole {

        @Test
        void demoteToUserSucceedsWhenOtherAdminsExist() {
            when(users.userExists("alice")).thenReturn(true);
            stubOtherEnabledAdmins("alice", 1);
            stubRowQuery("alice", "ROLE_ADMIN", true);

            UserSummary response = service.updateRole("alice", Role.USER);

            verify(jdbc).update("DELETE FROM authorities WHERE username = ?", "alice");
            verify(jdbc).update("INSERT INTO authorities (username, authority) VALUES (?, ?)", "alice", "ROLE_USER");
            assertThat(response.role()).isEqualTo(Role.USER);
        }

        @Test
        void promoteToAdminSucceedsAlways() {
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", true);

            UserSummary response = service.updateRole("bob", Role.ADMIN);

            verify(jdbc).update("INSERT INTO authorities (username, authority) VALUES (?, ?)", "bob", "ROLE_ADMIN");
            assertThat(response.role()).isEqualTo(Role.ADMIN);
        }

        @Test
        void notFoundForUnknownUser() {
            when(users.userExists("ghost")).thenReturn(false);

            assertThatThrownBy(() -> service.updateRole("ghost", Role.USER)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class LastAdminGuard {

        @Test
        void demotionRefusedWhenNoOtherEnabledAdmin() {
            when(users.userExists("alice")).thenReturn(true);
            stubOtherEnabledAdmins("alice", 0);

            assertThatThrownBy(() -> service.updateRole("alice", Role.USER))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex ->
                            assertThat(((ConflictException) ex).getErrorCode()).isEqualTo("last_admin"));
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        void disablingRefusedWhenNoOtherEnabledAdmin() {
            when(users.userExists("alice")).thenReturn(true);
            stubRowQuery("alice", "ROLE_ADMIN", true);
            stubOtherEnabledAdmins("alice", 0);

            assertThatThrownBy(() -> service.updateEnabled("alice", false))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex ->
                            assertThat(((ConflictException) ex).getErrorCode()).isEqualTo("last_admin"));
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }

        @Test
        void deletionRefusedWhenLastAdmin() {
            when(users.userExists("alice")).thenReturn(true);
            stubRowQuery("alice", "ROLE_ADMIN", true);
            stubOtherEnabledAdmins("alice", 0);

            assertThatThrownBy(() -> service.deleteUser("alice"))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(ex ->
                            assertThat(((ConflictException) ex).getErrorCode()).isEqualTo("last_admin"));
            verify(users, never()).deleteUser(anyString());
        }

        @Test
        void disablingNonAdminBypassesLastAdminGuard() {
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", true);

            service.updateEnabled("bob", false);

            verify(jdbc).update("UPDATE users SET enabled = ? WHERE username = ?", false, "bob");
        }
    }

    @Nested
    class UpdateEnabled {

        @Test
        void enableUserSucceeds() {
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", false);

            UserSummary response = service.updateEnabled("bob", true);

            verify(jdbc).update("UPDATE users SET enabled = ? WHERE username = ?", true, "bob");
            assertThat(response.enabled()).isTrue();
        }

        @Test
        void notFoundForUnknownUser() {
            when(users.userExists("ghost")).thenReturn(false);

            assertThatThrownBy(() -> service.updateEnabled("ghost", true)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class SessionRevocation {

        @Test
        void demoteToUserExpiresLiveSessionsForThatPrincipal() {
            UserDetails principal = User.withUsername("alice")
                    .password("x")
                    .authorities("ROLE_ADMIN")
                    .build();
            SessionInformation aliceSession = mock(SessionInformation.class);
            when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.List.of(principal, "ignored-other"));
            when(sessionRegistry.getAllSessions(principal, false)).thenReturn(java.util.List.of(aliceSession));
            when(users.userExists("alice")).thenReturn(true);
            stubOtherEnabledAdmins("alice", 1);
            stubRowQuery("alice", "ROLE_ADMIN", true);

            service.updateRole("alice", Role.USER);

            verify(aliceSession).expireNow();
        }

        @Test
        void disableExpiresLiveSessions() {
            UserDetails principal = User.withUsername("bob")
                    .password("x")
                    .authorities("ROLE_USER")
                    .build();
            SessionInformation bobSession = mock(SessionInformation.class);
            when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.List.of(principal));
            when(sessionRegistry.getAllSessions(principal, false)).thenReturn(java.util.List.of(bobSession));
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", true);

            service.updateEnabled("bob", false);

            verify(bobSession).expireNow();
        }

        @Test
        void deleteExpiresLiveSessions() {
            UserDetails principal = User.withUsername("bob")
                    .password("x")
                    .authorities("ROLE_USER")
                    .build();
            SessionInformation bobSession = mock(SessionInformation.class);
            when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.List.of(principal));
            when(sessionRegistry.getAllSessions(principal, false)).thenReturn(java.util.List.of(bobSession));
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", true);

            service.deleteUser("bob");

            verify(bobSession).expireNow();
        }

        @Test
        void enablingDoesNotExpireSessions() {
            UserDetails principal = User.withUsername("bob")
                    .password("x")
                    .authorities("ROLE_USER")
                    .build();
            SessionInformation bobSession = mock(SessionInformation.class);
            when(sessionRegistry.getAllPrincipals()).thenReturn(java.util.List.of(principal));
            when(sessionRegistry.getAllSessions(principal, false)).thenReturn(java.util.List.of(bobSession));
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", false);

            service.updateEnabled("bob", true);

            // Re-enabling a user must NOT invalidate their (already-absent) sessions — there is
            // nothing to revoke and calling expireNow() on an inactive registry would mask
            // real bugs in the revocation path.
            verify(bobSession, never()).expireNow();
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void deleteNonAdminSucceeds() {
            when(users.userExists("bob")).thenReturn(true);
            stubRowQuery("bob", "ROLE_USER", true);

            service.deleteUser("bob");

            verify(users).deleteUser("bob");
        }

        @Test
        void notFoundForUnknownUser() {
            when(users.userExists("ghost")).thenReturn(false);

            assertThatThrownBy(() -> service.deleteUser("ghost")).isInstanceOf(NotFoundException.class);
        }
    }

    private void stubOtherEnabledAdmins(String username, int count) {
        when(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM users u JOIN authorities a ON a.username = u.username"
                                + " WHERE a.authority = 'ROLE_ADMIN' AND u.enabled = TRUE AND u.username <> ?",
                        Integer.class,
                        username))
                .thenReturn(count);
    }

    private void stubRowQuery(String username, String authority, boolean enabled) {
        UserDetails details = mock(UserDetails.class);
        when(details.getUsername()).thenReturn(username);
        when(details.isEnabled()).thenReturn(enabled);
        when(details.getAuthorities())
                .thenAnswer(inv -> java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(authority)));
        when(users.loadUserByUsername(username)).thenReturn(details);
    }

    @SuppressWarnings("unused") // hold for future tests referencing UsernameNotFoundException
    private static final Class<?> ENSURE_NOT_FOUND_TYPE_AVAILABLE = UsernameNotFoundException.class;
}
