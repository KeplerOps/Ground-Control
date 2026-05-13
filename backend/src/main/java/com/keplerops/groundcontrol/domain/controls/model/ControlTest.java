package com.keplerops.groundcontrol.domain.controls.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Control test evidence record per GC-I012.
 *
 * <p>A durable, audited row capturing the result of a single test executed against a {@link
 * Control} at a point in time. Owns methodology (PCAOB AS 2201 vocabulary), test steps,
 * expected/actual results, conclusion, tester identity, and test date. Linkability to the control
 * is via the {@code control} FK; the row itself is the authoritative evidence artifact.
 *
 * <p>Separate from {@code ControlEffectivenessAssessment} (rating), {@code VerificationResult}
 * (requirement evidence), {@code Observation} (asset fact), and {@code RiskAssessmentResult}
 * (methodology-specific risk computation). See
 * {@code architecture/notes/control-testing-entity-preflight.md}.
 *
 * <p>{@code testerIdentity} is a domain-provenance field; it does not replace the authenticated
 * audit actor on the revision record.
 */
@Entity
@Audited
@Table(name = "control_test", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class ControlTest extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Control is itself @Audited, so this relation is audited normally; the audit table carries
    // control_id so revision history can resolve which control an old row referenced even after
    // the parent row is updated or deleted.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Column(nullable = false, length = 50)
    private String uid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ControlTestMethodology methodology;

    @Column(name = "test_steps", nullable = false, columnDefinition = "TEXT")
    private String testSteps;

    @Column(name = "expected_results", nullable = false, columnDefinition = "TEXT")
    private String expectedResults;

    @Column(name = "actual_results", nullable = false, columnDefinition = "TEXT")
    private String actualResults;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ControlTestConclusion conclusion;

    @Column(name = "tester_identity", nullable = false, length = 200)
    private String testerIdentity;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected ControlTest() {
        // JPA
    }

    public ControlTest(
            Project project,
            Control control,
            String uid,
            ControlTestMethodology methodology,
            String testSteps,
            String expectedResults,
            String actualResults,
            ControlTestConclusion conclusion,
            String testerIdentity,
            LocalDate testDate) {
        this.project = project;
        this.control = control;
        this.uid = uid;
        this.methodology = methodology;
        this.testSteps = testSteps;
        this.expectedResults = expectedResults;
        this.actualResults = actualResults;
        this.conclusion = conclusion;
        this.testerIdentity = testerIdentity;
        this.testDate = testDate;
    }

    public Project getProject() {
        return project;
    }

    public Control getControl() {
        return control;
    }

    public String getUid() {
        return uid;
    }

    public ControlTestMethodology getMethodology() {
        return methodology;
    }

    public void setMethodology(ControlTestMethodology methodology) {
        this.methodology = methodology;
    }

    public String getTestSteps() {
        return testSteps;
    }

    public void setTestSteps(String testSteps) {
        this.testSteps = testSteps;
    }

    public String getExpectedResults() {
        return expectedResults;
    }

    public void setExpectedResults(String expectedResults) {
        this.expectedResults = expectedResults;
    }

    public String getActualResults() {
        return actualResults;
    }

    public void setActualResults(String actualResults) {
        this.actualResults = actualResults;
    }

    public ControlTestConclusion getConclusion() {
        return conclusion;
    }

    public void setConclusion(ControlTestConclusion conclusion) {
        this.conclusion = conclusion;
    }

    public String getTesterIdentity() {
        return testerIdentity;
    }

    public void setTesterIdentity(String testerIdentity) {
        this.testerIdentity = testerIdentity;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public void setTestDate(LocalDate testDate) {
        this.testDate = testDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
