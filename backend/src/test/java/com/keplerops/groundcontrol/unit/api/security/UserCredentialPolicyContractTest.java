package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.shared.security.UserCredentialPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Static post-condition: the {@link UserCredentialPolicy#USERNAME_REGEX Java username regex}
 * must equal the SQL CHECK regex in V059. The constants in
 * {@link UserCredentialPolicy} flow through {@code CreateUserRequest}, {@code UserAdminController},
 * {@code UserAdminService}, and {@code FirstAdminBootstrapRunner}, so this single
 * Java-vs-SQL equivalence assertion covers every Java callsite. The structural test is the
 * mechanism the {@code .gc/plan-rules.md} pattern-drift policy normally expects from
 * cross-layer enums; here we apply the same shape to credential validation.
 */
class UserCredentialPolicyContractTest {

    /**
     * Extracts the regex literal out of {@code V059__create_users.sql}'s
     * {@code username ~ '<regex>'} clause. The grep stays narrow so unrelated changes to the
     * migration don't accidentally match.
     */
    private static final Pattern SQL_CHECK_REGEX_EXTRACTOR = Pattern.compile("username\\s+~\\s+'(?<regex>[^']+)'");

    private static final Path V059_PATH =
            Path.of("src", "main", "resources", "db", "migration", "V059__create_users.sql");

    @Test
    void javaAndSqlUsernameRegexAreIdentical() throws IOException {
        String migration = Files.readString(V059_PATH, StandardCharsets.UTF_8);
        Matcher m = SQL_CHECK_REGEX_EXTRACTOR.matcher(migration);
        assertThat(m.find())
                .as("V059 migration must contain the username regex CHECK")
                .isTrue();
        String sqlRegex = m.group("regex");

        // Postgres regex syntax is a superset of Java but the subset we use here (anchored
        // ^…$, character classes, quantifier) is portable. Asserting literal equality is the
        // simplest contract; if a future change makes the two intentionally diverge (e.g.,
        // adding a case-insensitive flag), this test forces the operator to update both sides
        // and document the divergence.
        assertThat(sqlRegex).isEqualTo(UserCredentialPolicy.USERNAME_REGEX);
    }
}
