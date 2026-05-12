package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.shared.security.FirstAdminBootstrapRunner;
import com.keplerops.groundcontrol.shared.security.FirstAdminBootstrapRunner.BootstrapExit;
import com.keplerops.groundcontrol.shared.security.FirstAdminBootstrapRunner.PasswordSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

/**
 * Drives the first-admin bootstrap runner with synthetic {@link ApplicationArguments}.
 *
 * <p>The runner is the highest-risk surface in this PR: it accepts a password on a startup
 * machine and writes a privileged credential to a permanent store. The tests assert the
 * production-safety guards from ADR-037 §5: no {@code --password=} argv path, password comes
 * only from env / file / stdin, the runner is idempotent on a repeat invocation, and it always
 * exits with a meaningful code instead of silently dropping into the web server when an
 * operator clearly intended to bootstrap.
 */
class FirstAdminBootstrapRunnerTest {

    private JdbcUserDetailsManager users;
    private PasswordEncoder encoder;
    private RecordingExit exit;
    private FixedPasswordSource passwordSource;

    @BeforeEach
    void setUp() {
        users = mock(JdbcUserDetailsManager.class);
        encoder = new BCryptPasswordEncoder();
        exit = new RecordingExit();
        passwordSource = new FixedPasswordSource(null);
    }

    @Nested
    class Skipped {

        @Test
        void absentCreateAdminFlagSkipsBootstrap() {
            runner().run(args());

            verify(users, never()).userExists(anyString());
            verify(users, never()).createUser(any());
            assertThat(exit.calls).isEmpty();
        }
    }

    @Nested
    class ArgvPasswordGuard {

        @Test
        void passwordArgvForbidden_evenWhenValueLooksReasonable() {
            // ADR-037 §5: --password= would leak the secret to shell history, process listings,
            // /proc, and CI logs. The runner refuses outright instead of "fixing it up" — being
            // permissive here would silently undermine the policy.
            runner().run(args("--create-admin", "--username=alice", "--password=correct-horse-battery"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }
    }

    @Nested
    class Validation {

        @Test
        void missingUsernameFailsFast() {
            runner().run(args("--create-admin"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }

        @Test
        void invalidUsernameFailsFast() {
            runner().run(args("--create-admin", "--username=Bad Name"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }

        @Test
        void missingPasswordFailsFast() {
            when(users.userExists("alice")).thenReturn(false);

            runner().run(args("--create-admin", "--username=alice"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }

        @Test
        void shortPasswordFailsFast() {
            when(users.userExists("alice")).thenReturn(false);
            passwordSource = new FixedPasswordSource("short".toCharArray());

            runner().run(args("--create-admin", "--username=alice"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }
    }

    @Nested
    class PasswordFile {

        @Test
        void worldReadablePasswordFileIsRefused(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("admin-password");
            Files.writeString(file, "correct-horse-battery-staple\n");
            try {
                Files.setPosixFilePermissions(
                        file,
                        EnumSet.of(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OTHERS_READ));
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem in the test sandbox — the fail-closed gate cannot be
                // exercised here. Documented in DEPLOYMENT.md.
                return;
            }
            when(users.userExists("alice")).thenReturn(false);

            // DefaultPasswordSource reads --password-file authoritatively (the cycle-2 fix:
            // when --password-file is supplied, the env / console fallbacks never fire). Use
            // the production source directly to exercise the POSIX check. The assertion is
            // unconditional — if `GC_ADMIN_BOOTSTRAP_PASSWORD` happens to be set in the test
            // env it is irrelevant because the file-flag short-circuit consumes the file path
            // alone.
            FirstAdminBootstrapRunner.DefaultPasswordSource source =
                    new FirstAdminBootstrapRunner.DefaultPasswordSource();
            Optional<char[]> result = source.read(
                    new DefaultApplicationArguments(
                            "--create-admin", "--username=alice", "--password-file=" + file.toAbsolutePath()),
                    List.of("alice"));
            org.assertj.core.api.Assertions.assertThat(result)
                    .as("World-readable password file must not produce a password")
                    .isEmpty();
        }

        @Test
        void ownerOnlyReadableFileIsAccepted(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("admin-password");
            Files.writeString(file, "correct-horse-battery-staple\n");
            try {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                return;
            }

            FirstAdminBootstrapRunner.DefaultPasswordSource source =
                    new FirstAdminBootstrapRunner.DefaultPasswordSource();
            Optional<char[]> result = source.read(
                    new DefaultApplicationArguments(
                            "--create-admin", "--username=alice", "--password-file=" + file.toAbsolutePath()),
                    List.of("alice"));
            // --password-file is authoritative, so the returned chars MUST be the file's
            // content. Strip the trailing newline the test wrote alongside the password.
            org.assertj.core.api.Assertions.assertThat(result)
                    .as("Mode-600 password file must be accepted and its content returned")
                    .isPresent();
            org.assertj.core.api.Assertions.assertThat(new String(result.get()))
                    .as("Returned chars must match the file's content")
                    .isEqualTo("correct-horse-battery-staple");
        }
    }

    @Nested
    class Bootstrap {

        @Test
        void createsAdminWithBcryptHashAndAdminAuthority() {
            when(users.userExists("alice")).thenReturn(false);
            passwordSource = new FixedPasswordSource("correct-horse-battery-staple".toCharArray());

            runner().run(args("--create-admin", "--username=alice"));

            verify(users)
                    .createUser(org.mockito.ArgumentMatchers.argThat((UserDetails u) -> "alice".equals(u.getUsername())
                            && encoder.matches("correct-horse-battery-staple", u.getPassword())
                            && u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                            && u.isEnabled()));
            assertThat(exit.calls).containsExactly(0);
        }

        @Test
        void idempotentWhenUserAlreadyExistsAsEnabledAdmin() {
            when(users.userExists("alice")).thenReturn(true);
            org.springframework.security.core.userdetails.UserDetails existing =
                    org.springframework.security.core.userdetails.User.withUsername("alice")
                            .password("hashed")
                            .authorities("ROLE_ADMIN")
                            .build();
            when(users.loadUserByUsername("alice")).thenReturn(existing);

            runner().run(args("--create-admin", "--username=alice"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(0);
        }

        @Test
        void failsWhenExistingUserIsNotAnAdmin() {
            when(users.userExists("alice")).thenReturn(true);
            org.springframework.security.core.userdetails.UserDetails existing =
                    org.springframework.security.core.userdetails.User.withUsername("alice")
                            .password("hashed")
                            .authorities("ROLE_USER")
                            .build();
            when(users.loadUserByUsername("alice")).thenReturn(existing);

            runner().run(args("--create-admin", "--username=alice"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }

        @Test
        void failsWhenExistingUserIsDisabled() {
            when(users.userExists("alice")).thenReturn(true);
            org.springframework.security.core.userdetails.UserDetails existing =
                    org.springframework.security.core.userdetails.User.withUsername("alice")
                            .password("hashed")
                            .authorities("ROLE_ADMIN")
                            .disabled(true)
                            .build();
            when(users.loadUserByUsername("alice")).thenReturn(existing);

            runner().run(args("--create-admin", "--username=alice"));

            verify(users, never()).createUser(any());
            assertThat(exit.calls).containsExactly(2);
        }

        @Test
        void wipesPasswordBufferAfterUse() {
            when(users.userExists("alice")).thenReturn(false);
            char[] buffer = "correct-horse-battery-staple".toCharArray();
            passwordSource = new FixedPasswordSource(buffer);

            runner().run(args("--create-admin", "--username=alice"));

            // Every position must be NUL after the runner returns; otherwise the secret lingers
            // in heap memory for the rest of the JVM's life.
            for (char c : buffer) {
                assertThat(c).isEqualTo('\0');
            }
        }
    }

    private FirstAdminBootstrapRunner runner() {
        return new FirstAdminBootstrapRunner(users, encoder, passwordSource, exit);
    }

    private static ApplicationArguments args(String... values) {
        return new DefaultApplicationArguments(values);
    }

    private static final class RecordingExit implements BootstrapExit {
        final java.util.List<Integer> calls = new java.util.ArrayList<>();

        @Override
        public void exit(int code) {
            calls.add(code);
        }
    }

    private static final class FixedPasswordSource implements PasswordSource {
        private final char[] password;

        FixedPasswordSource(char[] password) {
            this.password = password;
        }

        @Override
        public Optional<char[]> read(ApplicationArguments args, List<String> usernames) {
            return Optional.ofNullable(password);
        }
    }
}
