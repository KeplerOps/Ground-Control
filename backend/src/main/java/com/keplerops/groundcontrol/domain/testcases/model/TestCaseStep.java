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

@Entity
@Audited
@Table(name = "test_case_step", uniqueConstraints = @UniqueConstraint(columnNames = {"test_case_id", "step_number"}))
public class TestCaseStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    @Column(name = "expected_result", nullable = false, columnDefinition = "TEXT")
    private String expectedResult;

    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;

    protected TestCaseStep() {
        // JPA
    }

    public TestCaseStep(TestCase testCase, int stepNumber, String action, String expectedResult) {
        if (testCase == null) {
            throw new DomainValidationException("Test case must not be null", "invalid_test_case_step", Map.of());
        }
        if (stepNumber <= 0) {
            throw new DomainValidationException(
                    "Step number must be positive",
                    "invalid_test_case_step",
                    Map.of("stepNumber", String.valueOf(stepNumber)));
        }
        requireNonBlank(action, "Action");
        requireNonBlank(expectedResult, "Expected result");
        this.testCase = testCase;
        this.stepNumber = stepNumber;
        this.action = action;
        this.expectedResult = expectedResult;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        if (stepNumber <= 0) {
            throw new DomainValidationException(
                    "Step number must be positive",
                    "invalid_test_case_step",
                    Map.of("stepNumber", String.valueOf(stepNumber)));
        }
        this.stepNumber = stepNumber;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        requireNonBlank(action, "Action");
        this.action = action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        requireNonBlank(expectedResult, "Expected result");
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    private static void requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(fieldLabel + " must not be blank", "invalid_test_case_step", Map.of());
        }
    }
}
