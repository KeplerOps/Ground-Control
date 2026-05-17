package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * TC-005 / ADR-043 — Test repository organisation aggregate.
 *
 * <p>Folders are project-scoped and self-referencing; {@code parent == null}
 * means the folder sits at the project root. Sibling ordering is
 * container-local via {@link #sortOrder}, and sibling-title uniqueness is
 * enforced by partial unique indexes at the database (see V080).
 */
@Entity
@Audited
@Table(name = "test_case_folder")
public class TestCaseFolder extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TestCaseFolder parent;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TestCaseFolder() {
        // JPA
    }

    public TestCaseFolder(Project project, TestCaseFolder parent, String title, String description, int sortOrder) {
        if (project == null) {
            throw new DomainValidationException("Project must not be null", "invalid_test_case_folder", Map.of());
        }
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("Title must not be blank", "invalid_test_case_folder", Map.of());
        }
        if (sortOrder < 0) {
            throw new DomainValidationException(
                    "Sort order must be non-negative", "invalid_test_case_folder", Map.of());
        }
        this.project = project;
        this.parent = parent;
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public Project getProject() {
        return project;
    }

    public TestCaseFolder getParent() {
        return parent;
    }

    public void setParent(TestCaseFolder parent) {
        this.parent = parent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new DomainValidationException("Title must not be blank", "invalid_test_case_folder", Map.of());
        }
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        if (sortOrder < 0) {
            throw new DomainValidationException(
                    "Sort order must be non-negative", "invalid_test_case_folder", Map.of());
        }
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return title;
    }
}
