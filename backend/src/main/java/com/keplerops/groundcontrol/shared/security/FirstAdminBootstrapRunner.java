package com.keplerops.groundcontrol.shared.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * Bootstrap the first admin user on a locked-down install.
 *
 * <p>Activated only when the application is started with {@code --create-admin --username=NAME}.
 * The password is read from (in order):
 *
 * <ol>
 *   <li>{@code --password-file=/path/to/file} (preferred for one-shot scripted bootstraps; the
 *       file's POSIX permissions are checked and the runner warns if they are not 600).
 *   <li>{@code GC_ADMIN_BOOTSTRAP_PASSWORD} environment variable (preferred for secret-store
 *       integrations).
 *   <li>An interactive {@link java.io.Console} prompt, if a real console is attached.
 * </ol>
 *
 * <p>The runner refuses the {@code --password=...} argv shortcut outright (ADR-037 §5): an
 * argv-supplied password would land in shell history, {@code /proc/<pid>/cmdline}, process
 * listings, and any CI / agent transcript that captures the launch line. Allowing it for "just
 * local dev" undermines the policy because operators copy-paste the dev invocation into
 * production scripts.
 *
 * <p>After a successful (or no-op idempotent) bootstrap the runner exits the JVM. The
 * {@code --create-admin} invocation is meant to be a one-shot — falling through into a normally
 * running web server with the admin password buffer still hot in memory would defeat the
 * secret-handling discipline the runner exists to enforce.
 */
@Component
public class FirstAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirstAdminBootstrapRunner.class);
    // Credential policy is centralized at UserCredentialPolicy so the bootstrap runner cannot
    // drift from the REST surface and the database CHECK.
    private static final Pattern USERNAME_PATTERN = UserCredentialPolicy.USERNAME_PATTERN;
    private static final String CREATE_ADMIN_OPTION = "create-admin";
    private static final String USERNAME_OPTION = "username";
    private static final String PASSWORD_OPTION = "password";
    private static final String PASSWORD_FILE_OPTION = "password-file";
    private static final String ENV_PASSWORD = "GC_ADMIN_BOOTSTRAP_PASSWORD";
    private static final int MIN_PASSWORD_LENGTH = UserCredentialPolicy.MIN_PASSWORD_LENGTH;
    private static final int MAX_PASSWORD_LENGTH = UserCredentialPolicy.MAX_PASSWORD_LENGTH;
    private static final int EXIT_OK = 0;
    private static final int EXIT_USER_ERROR = 2;

    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final PasswordSource passwordSource;
    private final BootstrapExit exit;

    @Autowired
    public FirstAdminBootstrapRunner(JdbcUserDetailsManager users, PasswordEncoder encoder) {
        this(users, encoder, new DefaultPasswordSource(), new SystemBootstrapExit());
    }

    public FirstAdminBootstrapRunner(
            JdbcUserDetailsManager users, PasswordEncoder encoder, PasswordSource passwordSource, BootstrapExit exit) {
        this.users = users;
        this.encoder = encoder;
        this.passwordSource = passwordSource;
        this.exit = exit;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(CREATE_ADMIN_OPTION)) {
            return;
        }
        if (args.containsOption(PASSWORD_OPTION)) {
            log.error(
                    "Refusing --password argv (ADR-037 §5). Supply the password via {} env, --password-file=PATH, or stdin prompt.",
                    ENV_PASSWORD);
            exit.exit(EXIT_USER_ERROR);
            return;
        }
        List<String> usernameValues = args.getOptionValues(USERNAME_OPTION);
        if (usernameValues == null || usernameValues.isEmpty()) {
            log.error("--create-admin requires --username=<name>");
            exit.exit(EXIT_USER_ERROR);
            return;
        }
        String username = usernameValues.get(0);
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            log.error("--username does not match required pattern {}", USERNAME_PATTERN.pattern());
            exit.exit(EXIT_USER_ERROR);
            return;
        }
        if (users.userExists(username)) {
            // Idempotency is conditional: the no-op path must actually leave the install in a
            // bootstrappable state. A previous partial bootstrap, manual DB intervention, or
            // failed authority insert can leave a {@code users} row without an admin authority
            // — silently exiting 0 in that case would hide the lockout.
            var existing = users.loadUserByUsername(username);
            boolean isEnabledAdmin = existing.isEnabled()
                    && existing.getAuthorities().stream()
                            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            if (isEnabledAdmin) {
                log.info("Admin user '{}' already present and is an enabled admin; bootstrap is a no-op", username);
                exit.exit(EXIT_OK);
                return;
            }
            log.error(
                    "User '{}' exists but is not an enabled admin (enabled={}, authorities={}). "
                            + "Repair manually (DB or UserAdminService) or rerun with --username=<other>.",
                    username,
                    existing.isEnabled(),
                    existing.getAuthorities());
            exit.exit(EXIT_USER_ERROR);
            return;
        }
        char[] password = passwordSource.read(args, usernameValues).orElse(null);
        if (password == null) {
            log.error(
                    "No password supplied. Set {} env, --password-file=PATH, or run interactively from a TTY.",
                    ENV_PASSWORD);
            exit.exit(EXIT_USER_ERROR);
            return;
        }
        // exitCode is captured inside the try and dispatched OUTSIDE the finally so the
        // password buffer is actually zeroed in production. The production exit shim calls
        // System.exit, which never returns; if the dispatch sat inside the try the JVM would
        // terminate before reaching the finally and the secret would linger in heap memory
        // until the kernel reclaimed it. (Tests use a returning exit shim and saw the cleanup
        // — that masked the production divergence.) The branches use an if/else, NOT an early
        // return, so the finally always runs and the post-try dispatch is always reached.
        int exitCode;
        try {
            if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
                log.error(
                        "Password length is out of range; must be {}-{} characters",
                        MIN_PASSWORD_LENGTH,
                        MAX_PASSWORD_LENGTH);
                exitCode = EXIT_USER_ERROR;
            } else {
                String hashed = encoder.encode(new String(password));
                UserDetails details = User.withUsername(username)
                        .password(hashed)
                        .authorities("ROLE_ADMIN")
                        .disabled(false)
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .build();
                users.createUser(details);
                log.info("Created admin user '{}'", username);
                exitCode = EXIT_OK;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
        exit.exit(exitCode);
    }

    /**
     * Decoupled password source so unit tests can drive the runner without touching env vars,
     * the filesystem, or {@link java.io.Console}. The {@link ApplicationArguments} parameter
     * carries the file path; {@code usernames} is supplied for diagnostics only.
     */
    @FunctionalInterface
    public interface PasswordSource {
        Optional<char[]> read(ApplicationArguments args, List<String> usernames);
    }

    /** Exit shim so unit tests can record calls instead of terminating the JVM. */
    @FunctionalInterface
    public interface BootstrapExit {
        void exit(int code);
    }

    /**
     * Production password source: prefers {@code --password-file=PATH}, then the
     * {@code GC_ADMIN_BOOTSTRAP_PASSWORD} env var, then an interactive prompt on a TTY.
     */
    public static final class DefaultPasswordSource implements PasswordSource {

        @Override
        public Optional<char[]> read(ApplicationArguments args, List<String> usernames) {
            // --password-file is authoritative when supplied: if the file is rejected or
            // unreadable, the runner must fail rather than silently fall through to env /
            // console. Otherwise a stale GC_ADMIN_BOOTSTRAP_PASSWORD or an unexpected console
            // prompt can create the admin with a different secret than the operator selected.
            if (args.containsOption(PASSWORD_FILE_OPTION)) {
                return readFile(args);
            }
            return readEnv().or(this::readConsole);
        }

        private Optional<char[]> readFile(ApplicationArguments args) {
            List<String> values = args.getOptionValues(PASSWORD_FILE_OPTION);
            if (values == null || values.isEmpty()) {
                return Optional.empty();
            }
            Path path = Path.of(values.get(0));
            try {
                if (!Files.isRegularFile(path)) {
                    log.error("Refusing password file {} — not a regular file.", path);
                    return Optional.empty();
                }
                if (!isOwnerOnlyAccessible(path)) {
                    log.error(
                            "Refusing password file {} — group/others have read or write permission. Run chmod 600 first.",
                            path);
                    return Optional.empty();
                }
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String trimmed = stripTrailingNewline(content);
                return Optional.of(trimmed.toCharArray());
            } catch (IOException ex) {
                log.error("Failed to read password file {}: {}", path, ex.getMessage());
                return Optional.empty();
            }
        }

        /**
         * Fail-closed POSIX permission check. ADR-037 §5: the bootstrap creates a permanent
         * privileged credential, so accepting a group/world-readable OR group/world-writable
         * password file would let any local unprivileged user either snapshot the secret
         * (read) or replace it with an attacker-chosen value before the operator runs the
         * bootstrap (write). {@code true} only when neither group nor others have any of
         * read / write / execute on a POSIX filesystem; on non-POSIX filesystems we cannot
         * enforce the gate and DEPLOYMENT.md documents the operator-side requirement.
         */
        private static boolean isOwnerOnlyAccessible(Path path) {
            try {
                var attrs = Files.readAttributes(path, PosixFileAttributes.class);
                Set<PosixFilePermission> perms = attrs.permissions();
                return !perms.contains(PosixFilePermission.GROUP_READ)
                        && !perms.contains(PosixFilePermission.GROUP_WRITE)
                        && !perms.contains(PosixFilePermission.GROUP_EXECUTE)
                        && !perms.contains(PosixFilePermission.OTHERS_READ)
                        && !perms.contains(PosixFilePermission.OTHERS_WRITE)
                        && !perms.contains(PosixFilePermission.OTHERS_EXECUTE);
            } catch (UnsupportedOperationException | IOException ignored) {
                // Non-POSIX filesystem or unreadable attributes — treat as accepted because
                // we cannot enforce the gate. DEPLOYMENT.md documents the contract.
                return true;
            }
        }

        private Optional<char[]> readEnv() {
            String envValue = System.getenv(ENV_PASSWORD);
            if (envValue == null || envValue.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(envValue.toCharArray());
        }

        private Optional<char[]> readConsole() {
            var console = System.console();
            if (console == null) {
                return Optional.empty();
            }
            char[] entered = console.readPassword("Admin password: ");
            if (entered == null || entered.length == 0) {
                return Optional.empty();
            }
            return Optional.of(entered);
        }

        private static String stripTrailingNewline(String content) {
            int end = content.length();
            while (end > 0 && (content.charAt(end - 1) == '\n' || content.charAt(end - 1) == '\r')) {
                end--;
            }
            return content.substring(0, end);
        }
    }

    /** Production exit: calls {@link System#exit(int)} so the JVM tears down after bootstrap. */
    static final class SystemBootstrapExit implements BootstrapExit {
        @Override
        public void exit(int code) {
            System.exit(code);
        }
    }
}
