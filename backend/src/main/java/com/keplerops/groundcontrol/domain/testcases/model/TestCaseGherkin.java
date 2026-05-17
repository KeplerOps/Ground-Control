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

/**
 * BDD/Gherkin authored content for a {@link TestCase} whose {@code format} is
 * {@code GHERKIN} (TC-004 / ADR-042). One row per parent test case; the
 * {@code (test_case_id)} UNIQUE constraint enforces the cardinality at the
 * schema layer so concurrent inserts cannot create siblings.
 *
 * <p>The {@code source} column stores the canonical authored .feature text
 * verbatim. Parsing happens at the service boundary (validation only) — this
 * entity does not surface AST classes or parser internals.
 */
@Entity
@Audited
@Table(name = "test_case_gherkin", uniqueConstraints = @UniqueConstraint(columnNames = {"test_case_id"}))
public class TestCaseGherkin extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String source;

    protected TestCaseGherkin() {
        // JPA
    }

    public TestCaseGherkin(TestCase testCase, String source) {
        if (testCase == null) {
            throw new DomainValidationException("Test case must not be null", "invalid_test_case_gherkin", Map.of());
        }
        requireNonBlank(source);
        this.testCase = testCase;
        this.source = source;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        requireNonBlank(source);
        this.source = source;
    }

    private static void requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Source must not be blank", "invalid_test_case_gherkin", Map.of());
        }
    }
}
