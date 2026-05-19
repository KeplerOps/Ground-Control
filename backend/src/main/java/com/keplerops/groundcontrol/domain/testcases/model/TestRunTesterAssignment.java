package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * TC-008 / ADR-049 — One assigned tester on a {@link TestRun}.
 *
 * <p>Testers are domain-provenance values rather than principals in the
 * Spring Security {@code users} table: this row records who was assigned
 * to a run, not who is allowed to log in. Per ADR-037 the JDBC users /
 * authorities tables are session-credential storage and must not be
 * extended with business-user fields.
 *
 * <p>Tester name is project-bounded text (max 120 chars); the
 * {@code (test_run_id, tester_name)} pair is unique.
 */
@Entity
@Audited
@Table(
        name = "test_run_tester_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_run_id", "tester_name"}))
public class TestRunTesterAssignment extends BaseEntity {

    /**
     * Allow-listed character set for {@code tester_name}. Tester names round-trip
     * through {@code /api/v1/test-runs/{id}/testers/{testerName}} as a URL path
     * segment on remove, so any character a name carries on create must survive
     * a path-segment encode/decode round-trip. The set keeps letters, digits,
     * spaces, and a small punctuation set used in real human names; URL-reserved
     * characters (slash, question mark, hash, percent, etc.) are excluded.
     */
    private static final java.util.regex.Pattern TESTER_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9 _.\\-'@]+$");

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Column(name = "tester_name", nullable = false, length = 120)
    private String testerName;

    protected TestRunTesterAssignment() {
        // JPA
    }

    public TestRunTesterAssignment(TestRun testRun, String testerName) {
        if (testRun == null) {
            throw new DomainValidationException(
                    "Test run must not be null", "invalid_test_run_tester_assignment", Map.of());
        }
        if (testerName == null || testerName.isBlank()) {
            throw new DomainValidationException(
                    "Tester name must not be blank", "invalid_test_run_tester_assignment", Map.of());
        }
        if (!TESTER_NAME_PATTERN.matcher(testerName).matches()) {
            throw new DomainValidationException(
                    "Tester name contains unsupported characters",
                    "invalid_test_run_tester_assignment",
                    Map.of("allowed", "letters, digits, space, _ . - ' @"));
        }
        this.testRun = testRun;
        this.testerName = testerName;
    }

    public TestRun getTestRun() {
        return testRun;
    }

    public String getTesterName() {
        return testerName;
    }
}
