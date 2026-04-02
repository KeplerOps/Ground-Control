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
    void contextLoads() {
        // Spring context boots successfully with ddl-auto: validate
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
                        "027", "028", "029", "030", "031", "032", "033", "034", "035", "036");
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
        entityManager.createNativeQuery("SELECT 1 FROM asset_relation LIMIT 1").getResultList();
        entityManager
                .createNativeQuery("SELECT 1 FROM asset_relation_audit LIMIT 1")
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
    }
}
