package com.keplerops.groundcontrol.domain.testcases.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
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
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * TC-006 / ADR-044 — Top-level planning container for a testing effort.
 *
 * <p>A {@code TestPlan} carries scope metadata (name, description), release
 * coordinates (product, version, build) as bounded scalar text, a lifecycle
 * status, and planned schedule dates. The aggregate is intentionally flat —
 * plans do not nest — and the stable UUID primary key is the load-bearing
 * extensibility seam: future {@code TestRun} rows will FK back via
 * {@code test_run.test_plan_id} so a plan can group many runs without
 * carrying run IDs on itself.
 */
@Entity
@Audited
@Table(name = "test_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "uid"}))
public class TestPlan extends BaseEntity {

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

    @Column(length = 200)
    private String product;

    @Column(length = 100)
    private String version;

    @Column(length = 100)
    private String build;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPlanStatus status = TestPlanStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected TestPlan() {
        // JPA
    }

    public TestPlan(Project project, String uid, String name) {
        if (project == null) {
            throw new DomainValidationException("Project must not be null", "invalid_test_plan", Map.of());
        }
        if (uid == null || uid.isBlank()) {
            throw new DomainValidationException("UID must not be blank", "invalid_test_plan", Map.of());
        }
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_plan", Map.of());
        }
        this.project = project;
        this.uid = uid;
        this.name = name;
    }

    public void transitionStatus(TestPlanStatus newStatus) {
        if (newStatus == null || !status.canTransitionTo(newStatus)) {
            throw new DomainValidationException(
                    "Cannot transition test plan status from " + status + " to " + newStatus,
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name must not be blank", "invalid_test_plan", Map.of());
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public TestPlanStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate != null && this.endDate != null && startDate.isAfter(this.endDate)) {
            throw new DomainValidationException(
                    "Start date must be on or before end date",
                    "invalid_test_plan_schedule",
                    Map.of("start", startDate.toString(), "end", this.endDate.toString()));
        }
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate != null && this.startDate != null && endDate.isBefore(this.startDate)) {
            throw new DomainValidationException(
                    "End date must be on or after start date",
                    "invalid_test_plan_schedule",
                    Map.of("start", this.startDate.toString(), "end", endDate.toString()));
        }
        this.endDate = endDate;
    }
}
