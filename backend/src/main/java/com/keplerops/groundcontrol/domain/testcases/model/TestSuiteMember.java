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
 * TC-007 / ADR-047 — A row in a {@link TestSuitePopulationMode#STATIC}
 * {@link TestSuite}'s explicit membership. Position is author-defined
 * and contiguous within a suite (the service compacts on remove /
 * reorder).
 */
@Entity
@Audited
@Table(
        name = "test_suite_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"test_suite_id", "test_case_id"}))
public class TestSuiteMember extends BaseEntity {

    // Identity-defining FKs stay in the audit shadow so a member-row
    // revision can be traced back to its parent suite and the linked test
    // case after the live row is deleted. The target entities are not
    // re-audited through this relation (Envers would otherwise chase the
    // suite/test-case audit graph from here).
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(nullable = false)
    private int position;

    protected TestSuiteMember() {
        // JPA
    }

    public TestSuiteMember(TestSuite testSuite, TestCase testCase, int position) {
        if (testSuite == null) {
            throw new DomainValidationException("Test suite must not be null", "invalid_test_suite_member", Map.of());
        }
        if (testCase == null) {
            throw new DomainValidationException("Test case must not be null", "invalid_test_suite_member", Map.of());
        }
        if (position < 0) {
            throw new DomainValidationException("Position must be non-negative", "invalid_test_suite_member", Map.of());
        }
        this.testSuite = testSuite;
        this.testCase = testCase;
        this.position = position;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < 0) {
            throw new DomainValidationException("Position must be non-negative", "invalid_test_suite_member", Map.of());
        }
        this.position = position;
    }
}
