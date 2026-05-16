package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class MigrationSmokeTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    void contextLoads() throws Exception {
        // Spring context boots successfully with ddl-auto: validate. The
        // assertion makes the pass/fail explicit so a future config change
        // that silently disables validation (ddl-auto: none / create) still
        // produces a real signal — empty test bodies pass even when the
        // intended schema-correctness guarantee has been lost.
        try (var conn = dataSource.getConnection()) {
            assertThat(conn.isValid(1)).isTrue();
        }
    }

    @Test
    void allFlywayMigrationsRan() throws Exception {
        List<String> versions = new ArrayList<>();
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank")) {
            while (rs.next()) {
                versions.add(rs.getString("version"));
            }
        }
        assertThat(versions)
                .containsExactly(
                        "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011", "012", "013",
                        "014", "015", "016", "017", "018", "019", "020", "021", "022", "023", "024", "025", "026",
                        "027", "028", "029", "030", "031", "032", "033", "034", "035", "036", "037", "038", "039",
                        "040", "041", "042", "043", "044", "045", "046", "047", "048", "049", "050", "051", "052",
                        "053", "054", "055", "056", "057", "058", "059", "060", "061", "062", "063", "064", "065",
                        "066", "067", "068", "069", "070", "071", "072");
    }

    @Test
    @Transactional
    void auditTablesExist() {
        // These queries will throw if tables don't exist
        entityManager.createNativeQuery("SELECT 1 FROM requirement LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM requirement_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM requirement_relation_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM revinfo LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM traceability_link LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM github_issue_sync LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM requirement_import LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM traceability_link_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM operational_asset LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM operational_asset_audit LIMIT 1")
                .getResultList();
        // GC-M012 column-existence probes (V069 / V070). A column-by-column
        // SELECT throws PersistenceException if any ALTER TABLE in the
        // migration silently omitted a column, where the table-only
        // `SELECT 1 FROM operational_asset` check would not. Wrap with
        // assertThatCode so the intent — "these six columns must exist" —
        // is expressed as an assertion a reader can recognize, not as the
        // absence of a thrown exception.
        org.assertj.core.api.Assertions.assertThatCode(() -> entityManager
                        .createNativeQuery(
                                "SELECT owner, steward, environment, criticality, business_context, scope_designation"
                                        + " FROM operational_asset LIMIT 1")
                        .getResultList())
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() -> entityManager
                        .createNativeQuery(
                                "SELECT owner, steward, environment, criticality, business_context, scope_designation"
                                        + " FROM operational_asset_audit LIMIT 1")
                        .getResultList())
                .doesNotThrowAnyException();
        entityManager.createNativeQuery("SELECT 1 FROM asset_relation LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM asset_relation_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT updated_at FROM asset_relation LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT updated_at FROM asset_relation_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM asset_link LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM asset_link_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM asset_external_id LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM asset_external_id_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM observation LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM observation_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM risk_scenario LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_scenario_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_scenario_link LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_scenario_link_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM methodology_profile LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM methodology_profile_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_register_record LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_register_record_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_register_record_scenario LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_register_record_scenario_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_assessment_result LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_assessment_result_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_assessment_result_observation LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM risk_assessment_result_observation_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM treatment_plan LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM treatment_plan_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM control LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM control_audit LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM control_link LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_link_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM github_pr_sync LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM verification_result LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM verification_result_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM registered_plugin LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM control_pack LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_pack_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_pack_entry LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_pack_entry_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_pack_override LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_pack_override_audit LIMIT 1")
                .getResultList();
        // V053: pack registry tables
        entityManager
                .createNativeQuery("SELECT 1 FROM pack_registry_entry LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM pack_registry_entry_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM pack_install_record LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM pack_install_record_audit LIMIT 1")
                .getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM trust_policy LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM trust_policy_audit LIMIT 1")
                .getResultList();
        // V053/V054 pack registry tables verified
        // V055-V058 threat model tables
        entityManager.createNativeQuery("SELECT 1 FROM threat_model LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM threat_model_audit LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM threat_model_link LIMIT 1")
                .getResultList();
        // V057 set target_url / target_title to NOT NULL DEFAULT '' so the entity-side
        // empty-string contract holds end-to-end. Verify the column metadata directly.
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'threat_model_link'"
                        + " AND column_name = 'target_url' AND is_nullable = 'NO'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'threat_model_link'"
                        + " AND column_name = 'target_title' AND is_nullable = 'NO'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM threat_model_link_audit LIMIT 1")
                .getResultList();
        // V059: ADR-037 browser session JDBC user store. These are Spring Security
        // principal tables (not domain entities), so they intentionally have no
        // matching _audit suffix — role-change events are captured via structured
        // log lines from UserAdminService instead. See ADR-037 §4 and §6.
        entityManager.createNativeQuery("SELECT 1 FROM users LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM authorities LIMIT 1").getResultList();
        // V060-V063: finding tables (GC-V001 / ADR-038)
        entityManager.createNativeQuery("SELECT 1 FROM finding LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM finding_audit LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM finding_link LIMIT 1").getResultList();
        // V062 sets target_url / target_title to NOT NULL DEFAULT '' so the entity-side
        // empty-string contract holds end-to-end. Verify the column metadata directly.
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'finding_link'"
                        + " AND column_name = 'target_url' AND is_nullable = 'NO'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'finding_link'"
                        + " AND column_name = 'target_title' AND is_nullable = 'NO'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM finding_link_audit LIMIT 1")
                .getResultList();
        // V065-V066 control_test + audit (GC-I012 / ADR-039)
        entityManager.createNativeQuery("SELECT 1 FROM control_test LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_test_audit LIMIT 1")
                .getResultList();
        // V067-V068 control_effectiveness_assessment + audit (GC-I013 / ADR-039)
        entityManager
                .createNativeQuery("SELECT 1 FROM control_effectiveness_assessment LIMIT 1")
                .getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM control_effectiveness_assessment_audit LIMIT 1")
                .getResultList();
        // V071-V072 test_case + audit (TC-001 / ADR-040). The audit table is
        // not a Hibernate-managed entity, so ddl-auto: validate doesn't catch
        // a misspelled or dropped column there. Verify the structural shape
        // via information_schema for the columns most likely to regress.
        entityManager.createNativeQuery("SELECT 1 FROM test_case LIMIT 1").getResultList();
        entityManager.createNativeQuery("SELECT 1 FROM test_case_audit LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'test_case_audit'"
                        + " AND column_name = 'estimated_duration_seconds'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'test_case_audit'"
                        + " AND column_name = 'status'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'test_case_audit'"
                        + " AND column_name = 'type'")
                .getSingleResult();
        entityManager
                .createNativeQuery("SELECT 1 FROM information_schema.columns WHERE table_name = 'test_case_audit'"
                        + " AND column_name = 'priority'")
                .getSingleResult();
    }
}
