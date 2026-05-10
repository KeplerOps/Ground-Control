package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Audit-actor provenance under security-enabled conditions — the evidence for issue #431 and
 * ADR-033.
 *
 * <p>Flips {@code groundcontrol.security.enabled=true} for this test only (the rest of the
 * integration suite runs with security disabled — the {@code test} profile default), configures
 * test credentials, and drives mutations with {@code Authorization: Bearer <token>}. Asserts the
 * cardinal contracts of #431:
 *
 * <ul>
 *   <li>Envers revision actor is the authenticated principal name, never a client-supplied
 *       string.
 *   <li>A spoofed {@code X-Actor} header does not override the authenticated principal.
 *   <li>An unauthenticated mutation is rejected by the security chain (401) before any controller
 *       runs, so neither a live row nor an {@code anonymous} Envers audit row is written — proven
 *       by querying {@code requirement} and {@code requirement_audit} directly, not by an
 *       (absence-based) live read.
 * </ul>
 *
 * <p>This test deliberately does <em>not</em> use {@code @Transactional}: Envers flushes audit
 * rows at transaction completion, so reading audit data back requires each request's transaction
 * to commit. Rows created here are removed in {@link #cleanup()}.
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(
        properties = {
            "groundcontrol.security.enabled=true",
            "groundcontrol.security.openapi-public=false",
            "groundcontrol.security.credentials[0].principal-name=alice",
            "groundcontrol.security.credentials[0].token=user-token-aaa",
            "groundcontrol.security.credentials[0].role=USER",
            "groundcontrol.security.credentials[1].principal-name=admin-bob",
            "groundcontrol.security.credentials[1].token=admin-token-bbb",
            "groundcontrol.security.credentials[1].role=ADMIN",
            "groundcontrol.security.ip-allowlist[0]=127.0.0.0/8",
            "groundcontrol.security.ip-allowlist[1]=::1/128"
        })
class AuditActorProvenanceIntegrationTest extends BaseIntegrationTest {

    private static final String USER_TOKEN = "Bearer user-token-aaa";
    private static final String ADMIN_TOKEN = "Bearer admin-token-bbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @AfterAll
    void cleanup() throws Exception {
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM requirement_audit WHERE uid LIKE 'AUTHPROV-%'");
            stmt.executeUpdate("DELETE FROM requirement WHERE uid LIKE 'AUTHPROV-%'");
        }
    }

    @Test
    void authenticatedMutation_recordsAuthenticatedPrincipalAsAuditActor() throws Exception {
        var id = createRequirement("AUTHPROV-001", USER_TOKEN, /* spoofedActor= */ null);

        mockMvc.perform(get("/api/v1/requirements/" + id + "/history").header("Authorization", USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].revisionType", is("ADD")))
                .andExpect(jsonPath("$[0].timestamp", notNullValue()))
                .andExpect(jsonPath("$[0].actor", is("alice")));

        // Belt-and-suspenders: the Envers audit row itself carries the authenticated principal.
        assertThat(auditActors("AUTHPROV-001")).containsExactly("alice");
    }

    @Test
    void spoofedXActorHeader_doesNotOverrideAuthenticatedPrincipal() throws Exception {
        var id = createRequirement("AUTHPROV-002", USER_TOKEN, /* spoofedActor= */ "attacker");

        mockMvc.perform(get("/api/v1/requirements/" + id + "/history").header("Authorization", USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor", is("alice")));

        assertThat(auditActors("AUTHPROV-002")).containsExactly("alice").doesNotContain("attacker");
    }

    @Test
    void unauthenticatedMutation_isRejected_andWritesNoAuditRevision() throws Exception {
        var createBody = Map.of(
                "uid", "AUTHPROV-003",
                "title", "Should never be persisted",
                "statement", "Unauthenticated mutation must be rejected before the controller runs",
                "requirementType", "FUNCTIONAL",
                "priority", "MUST");
        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("authentication_required")));

        // The security chain rejected the call before any controller ran: no live row AND no
        // Envers audit row (so no `anonymous` revision). Asserted directly against the tables,
        // not inferred from a live read (a missing live requirement would also hide an orphaned
        // audit row, so absence-of-live is the wrong observable for this contract).
        assertThat(count("SELECT COUNT(*) FROM requirement WHERE uid = ?", "AUTHPROV-003"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM requirement_audit WHERE uid = ?", "AUTHPROV-003"))
                .isZero();

        // And the live read path agrees.
        mockMvc.perform(get("/api/v1/requirements/uid/AUTHPROV-003").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("not_found")));
    }

    private UUID createRequirement(String uid, String bearerToken, String spoofedActor) throws Exception {
        var createBody = Map.of(
                "uid", uid,
                "title", "Provenance test " + uid,
                "statement", "Created under security-enabled conditions for #431 provenance coverage",
                "requirementType", "FUNCTIONAL",
                "priority", "MUST");
        var request = post("/api/v1/requirements")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody));
        if (spoofedActor != null) {
            request = request.header("X-Actor", spoofedActor);
        }
        var result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    /** Distinct Envers actor values recorded against a requirement uid, joined through {@code revinfo}. */
    private List<String> auditActors(String uid) throws Exception {
        var actors = new ArrayList<String>();
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("SELECT DISTINCT r.actor FROM requirement_audit ra "
                        + "JOIN revinfo r ON ra.rev = r.rev WHERE ra.uid = ? ORDER BY r.actor")) {
            ps.setString(1, uid);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    actors.add(rs.getString(1));
                }
            }
        }
        return actors;
    }

    private long count(String sql, String uid) throws Exception {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
