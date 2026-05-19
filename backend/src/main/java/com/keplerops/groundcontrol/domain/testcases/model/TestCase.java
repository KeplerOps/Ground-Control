package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
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
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@Entity
@Audited
@Table(name = "test_case", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class TestCase extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String preconditions;

    @Column(columnDefinition = "TEXT")
    private String postconditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCasePriority priority = TestCasePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseStatus status = TestCaseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseType type;

    /**
     * Authored format axis (ADR-042). Set on create and immutable thereafter:
     * once a parent has chosen a format, switching would orphan its children
     * (steps for {@code STEP_BASED}, Gherkin source for {@code GHERKIN}) and
     * break the format-vs-children invariant the services enforce. Existing
     * pre-TC-004 rows back-fill to {@code STEP_BASED} via V076's DEFAULT.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseFormat format = TestCaseFormat.STEP_BASED;

    @Column(name = "estimated_duration_seconds")
    private Long estimatedDurationSeconds;

    /**
     * TC-005 / ADR-043 — Hierarchical placement. {@code null} parent means
     * the test case sits at the project root. Sibling order within the
     * container is provided by {@link #sortOrder}; reorder operations
     * update only the affected container.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private TestCaseFolder parentFolder;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TestCase() {
        // JPA
    }

    public TestCase(Project project, String uid, String title, TestCaseType type, TestCasePriority priority) {
        this(project, uid, title, type, priority, TestCaseFormat.STEP_BASED);
    }

    public TestCase(
            Project project,
            String uid,
            String title,
            TestCaseType type,
            TestCasePriority priority,
            TestCaseFormat format) {
        if (project == null) {
            throw new DomainValidationException("Project must not be null", "invalid_test_case", Map.of());
        }
        if (uid == null || uid.isBlank()) {
            throw new DomainValidationException("UID must not be blank", "invalid_test_case", Map.of());
        }
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("Title must not be blank", "invalid_test_case", Map.of());
        }
        if (type == null) {
            throw new DomainValidationException("Type must not be null", "invalid_test_case", Map.of());
        }
        if (priority == null) {
            throw new DomainValidationException("Priority must not be null", "invalid_test_case", Map.of());
        }
        if (format == null) {
            throw new DomainValidationException("Format must not be null", "invalid_test_case", Map.of());
        }
        this.project = project;
        this.uid = uid;
        this.title = title;
        this.type = type;
        this.priority = priority;
        this.format = format;
    }

    public void transitionStatus(TestCaseStatus newStatus) {
        if (newStatus == null || !status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition test case status from " + status + " to " + newStatus,
                    "invalid_status_transition",
                    Map.of("current", status.name(), "requested", String.valueOf(newStatus)));
        }
        this.status = newStatus;
    }

    public Project getProject() {
        return project;
    }

    public String getUid() {
        return uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("Title must not be blank", "invalid_test_case", Map.of());
        }
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public String getPostconditions() {
        return postconditions;
    }

    public void setPostconditions(String postconditions) {
        this.postconditions = postconditions;
    }

    public TestCasePriority getPriority() {
        return priority;
    }

    public void setPriority(TestCasePriority priority) {
        if (priority == null) {
            throw new DomainValidationException("Priority must not be null", "invalid_test_case", Map.of());
        }
        this.priority = priority;
    }

    public TestCaseStatus getStatus() {
        return status;
    }

    public TestCaseType getType() {
        return type;
    }

    public void setType(TestCaseType type) {
        if (type == null) {
            throw new DomainValidationException("Type must not be null", "invalid_test_case", Map.of());
        }
        this.type = type;
    }

    public TestCaseFormat getFormat() {
        return format;
    }

    public Long getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Long estimatedDurationSeconds) {
        if (estimatedDurationSeconds != null && estimatedDurationSeconds < 0) {
            throw new DomainValidationException(
                    "Estimated duration must be non-negative", "invalid_test_case", Map.of());
        }
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public TestCaseFolder getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(TestCaseFolder parentFolder) {
        this.parentFolder = parentFolder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        if (sortOrder < 0) {
            throw new DomainValidationException("Sort order must be non-negative", "invalid_test_case", Map.of());
        }
        this.sortOrder = sortOrder;
    }
}
