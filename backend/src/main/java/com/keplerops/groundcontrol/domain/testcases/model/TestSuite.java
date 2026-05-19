package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * TC-007 / ADR-047 — Selection container for test cases inside a project.
 *
 * <p>The suite owns a single immutable {@link TestSuitePopulationMode}.
 * Each mode populates the suite from a different source — STATIC suites
 * own explicit {@code test_suite_member} rows, REQUIREMENTS_BASED suites
 * own {@code test_suite_source_requirement} rows resolved through
 * existing requirement/test traceability, QUERY_BASED suites own typed
 * criteria stored as columns here on the root and resolved against the
 * test-case repository at read time. Switching modes after create would
 * orphan member / source / criteria rows and break resolve-time
 * dispatch, so {@code population_mode} has no setter.
 */
@Entity
@Audited
@Table(name = "test_suite", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class TestSuite extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "population_mode", nullable = false, length = 20, updatable = false)
    private TestSuitePopulationMode populationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_status", length = 20)
    private TestCaseStatus criteriaStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", length = 20)
    private TestCaseType criteriaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_priority", length = 20)
    private TestCasePriority criteriaPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_format", length = 20)
    private TestCaseFormat criteriaFormat;

    // FK held as raw UUID rather than @ManyToOne TestCaseFolder so the suite
    // module does not import folder lifecycle. Folder existence and
    // same-project scope are validated in the service when the column is set
    // or updated.
    @Column(name = "criteria_folder_id")
    private UUID criteriaFolderId;

    @Column(name = "criteria_text_search", length = 200)
    private String criteriaTextSearch;

    protected TestSuite() {
        // JPA
    }

    public TestSuite(Project project, String uid, String name, TestSuitePopulationMode populationMode) {
        if (project == null) {
            throw new DomainValidationException("Project must not be null", "invalid_test_suite", Map.of());
        }
        if (uid == null || uid.isBlank()) {
            throw new DomainValidationException("UID must not be blank", "invalid_test_suite", Map.of());
        }
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_suite", Map.of());
        }
        if (populationMode == null) {
            throw new DomainValidationException("Population mode must not be null", "invalid_test_suite", Map.of());
        }
        this.project = project;
        this.uid = uid;
        this.name = name;
        this.populationMode = populationMode;
    }

    public Project getProject() {
        return project;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_suite", Map.of());
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestSuitePopulationMode getPopulationMode() {
        return populationMode;
    }

    public TestCaseStatus getCriteriaStatus() {
        return criteriaStatus;
    }

    public void setCriteriaStatus(TestCaseStatus criteriaStatus) {
        rejectCriteriaFieldOnNonQueryMode(criteriaStatus, "criteria_status");
        this.criteriaStatus = criteriaStatus;
    }

    public TestCaseType getCriteriaType() {
        return criteriaType;
    }

    public void setCriteriaType(TestCaseType criteriaType) {
        rejectCriteriaFieldOnNonQueryMode(criteriaType, "criteria_type");
        this.criteriaType = criteriaType;
    }

    public TestCasePriority getCriteriaPriority() {
        return criteriaPriority;
    }

    public void setCriteriaPriority(TestCasePriority criteriaPriority) {
        rejectCriteriaFieldOnNonQueryMode(criteriaPriority, "criteria_priority");
        this.criteriaPriority = criteriaPriority;
    }

    public TestCaseFormat getCriteriaFormat() {
        return criteriaFormat;
    }

    public void setCriteriaFormat(TestCaseFormat criteriaFormat) {
        rejectCriteriaFieldOnNonQueryMode(criteriaFormat, "criteria_format");
        this.criteriaFormat = criteriaFormat;
    }

    public UUID getCriteriaFolderId() {
        return criteriaFolderId;
    }

    public void setCriteriaFolderId(UUID criteriaFolderId) {
        rejectCriteriaFieldOnNonQueryMode(criteriaFolderId, "criteria_folder_id");
        this.criteriaFolderId = criteriaFolderId;
    }

    public String getCriteriaTextSearch() {
        return criteriaTextSearch;
    }

    public void setCriteriaTextSearch(String criteriaTextSearch) {
        rejectCriteriaFieldOnNonQueryMode(criteriaTextSearch, "criteria_text_search");
        this.criteriaTextSearch = criteriaTextSearch;
    }

    /**
     * True when at least one query criterion is set. QUERY_BASED suites must
     * satisfy this on create AND on every update so the resolve dispatch
     * never sees an unconstrained query that would return the project's
     * entire test-case corpus.
     */
    public boolean hasAnyCriteria() {
        return criteriaStatus != null
                || criteriaType != null
                || criteriaPriority != null
                || criteriaFormat != null
                || criteriaFolderId != null
                || (criteriaTextSearch != null && !criteriaTextSearch.isBlank());
    }

    /**
     * Reject a non-null criterion assignment when this suite is not in
     * {@link TestSuitePopulationMode#QUERY_BASED}. Null assignments are
     * allowed in any mode so partial-update "clear" operations work.
     */
    private void rejectCriteriaFieldOnNonQueryMode(Object value, String field) {
        if (value != null && populationMode != TestSuitePopulationMode.QUERY_BASED) {
            throw new DomainValidationException(
                    field + " is only valid for QUERY_BASED suites",
                    "invalid_test_suite_mode_field",
                    Map.of("mode", populationMode.name(), "field", field));
        }
    }
}
