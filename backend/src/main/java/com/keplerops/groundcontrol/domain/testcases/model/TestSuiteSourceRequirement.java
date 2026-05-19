package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
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
 * TC-007 / ADR-047 — A source requirement on a {@link
 * TestSuitePopulationMode#REQUIREMENTS_BASED} {@link TestSuite}. The
 * suite resolves member test cases from this row's requirement via the
 * existing {@code TraceabilityLink} model (linkType = TESTS,
 * artifactType = TEST) at read time; the source row is the rule, not
 * the cached outcome.
 */
@Entity
@Audited
@Table(
        name = "test_suite_source_requirement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_suite_id", "requirement_id"}))
public class TestSuiteSourceRequirement extends BaseEntity {

    // Identity-defining FKs stay in the audit shadow so a source-row
    // revision can be traced back to its parent suite and the linked
    // requirement after the live row is deleted. The target entities are
    // not re-audited through this relation.
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    protected TestSuiteSourceRequirement() {
        // JPA
    }

    public TestSuiteSourceRequirement(TestSuite testSuite, Requirement requirement) {
        if (testSuite == null) {
            throw new DomainValidationException(
                    "Test suite must not be null", "invalid_test_suite_source_requirement", Map.of());
        }
        if (requirement == null) {
            throw new DomainValidationException(
                    "Requirement must not be null", "invalid_test_suite_source_requirement", Map.of());
        }
        this.testSuite = testSuite;
        this.requirement = requirement;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public Requirement getRequirement() {
        return requirement;
    }
}
