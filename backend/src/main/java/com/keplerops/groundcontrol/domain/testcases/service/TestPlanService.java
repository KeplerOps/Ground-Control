package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.repository.TestPlanRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-006 / ADR-044 — Application service for {@link TestPlan}.
 *
 * <p>Resolves project scope through {@link ProjectService}, enforces
 * project-scoped UID uniqueness on create, partial-update semantics with
 * explicit {@code clear*} flags (TC-001 codex-cycle-1 contract), and
 * delegates status transition legality to the entity. Lifecycle logs are
 * low-cardinality structured lines per the preflight observability contract.
 */
@Service
@Transactional
public class TestPlanService {

    private static final Logger log = LoggerFactory.getLogger(TestPlanService.class);

    private final TestPlanRepository testPlanRepository;
    private final ProjectService projectService;

    public TestPlanService(TestPlanRepository testPlanRepository, ProjectService projectService) {
        this.testPlanRepository = testPlanRepository;
        this.projectService = projectService;
    }

    public TestPlan create(CreateTestPlanCommand command) {
        var project = projectService.getById(command.projectId());
        if (testPlanRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Test plan with UID " + command.uid() + " already exists in this project");
        }
        var plan = new TestPlan(project, command.uid(), command.name());
        plan.setDescription(command.description());
        plan.setProduct(command.product());
        plan.setVersion(command.version());
        plan.setBuild(command.build());
        // Order matters: setStartDate / setEndDate each compare against the
        // currently-stored counterpart, so seed the earlier endpoint first
        // to keep an inverted pair from passing under transient state.
        plan.setStartDate(command.startDate());
        plan.setEndDate(command.endDate());
        plan = testPlanRepository.save(plan);
        log.info(
                "test_plan_created: uid={} project={} id={} status={}",
                plan.getUid(),
                project.getIdentifier(),
                plan.getId(),
                plan.getStatus());
        return plan;
    }

    @Transactional(readOnly = true)
    public TestPlan getById(UUID projectId, UUID id) {
        return requirePlanInProject(projectId, id);
    }

    @Transactional(readOnly = true)
    public TestPlan getByUid(UUID projectId, String uid) {
        return testPlanRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Test plan not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<TestPlan> listByProject(UUID projectId) {
        return testPlanRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public TestPlan update(UUID projectId, UUID id, UpdateTestPlanCommand command) {
        var plan = requirePlanInProject(projectId, id);
        if (command.name() != null) {
            plan.setName(command.name());
        }
        if (command.clearDescription()) {
            plan.setDescription(null);
        } else if (command.description() != null) {
            plan.setDescription(command.description());
        }
        if (command.clearProduct()) {
            plan.setProduct(null);
        } else if (command.product() != null) {
            plan.setProduct(command.product());
        }
        if (command.clearVersion()) {
            plan.setVersion(null);
        } else if (command.version() != null) {
            plan.setVersion(command.version());
        }
        if (command.clearBuild()) {
            plan.setBuild(null);
        } else if (command.build() != null) {
            plan.setBuild(command.build());
        }
        // Validate the *final* start/end pair once, then assign both. The
        // entity setters each compare against the stored counterpart, so
        // setting them one at a time would reject a valid whole-window
        // shift (codex pre-push cycle 1 class finding): existing 6/1–6/30,
        // new 7/1–7/31 would trip when the new start (7/1) is compared to
        // the still-old end (6/30). Compute the target pair from the
        // current state + clear flags + new values, validate it once, then
        // null both fields and reassign so the setters' per-field checks
        // see a clean state.
        LocalDate targetStartDate;
        if (command.clearStartDate()) {
            targetStartDate = null;
        } else if (command.startDate() != null) {
            targetStartDate = command.startDate();
        } else {
            targetStartDate = plan.getStartDate();
        }
        LocalDate targetEndDate;
        if (command.clearEndDate()) {
            targetEndDate = null;
        } else if (command.endDate() != null) {
            targetEndDate = command.endDate();
        } else {
            targetEndDate = plan.getEndDate();
        }
        if (targetStartDate != null && targetEndDate != null && targetStartDate.isAfter(targetEndDate)) {
            throw new DomainValidationException(
                    "End date must be on or after start date",
                    "invalid_test_plan_schedule",
                    Map.of("start", targetStartDate.toString(), "end", targetEndDate.toString()));
        }
        // Clear both before reassigning so the setters' transient-pair
        // checks see (null, null) instead of stale state.
        plan.setStartDate(null);
        plan.setEndDate(null);
        plan.setStartDate(targetStartDate);
        plan.setEndDate(targetEndDate);
        plan = testPlanRepository.save(plan);
        log.info("test_plan_updated: id={} uid={}", plan.getId(), plan.getUid());
        return plan;
    }

    public TestPlan transitionStatus(UUID projectId, UUID id, TestPlanStatus newStatus) {
        var plan = requirePlanInProject(projectId, id);
        plan.transitionStatus(newStatus);
        plan = testPlanRepository.save(plan);
        log.info("test_plan_status_changed: id={} uid={} status={}", plan.getId(), plan.getUid(), plan.getStatus());
        return plan;
    }

    public void delete(UUID projectId, UUID id) {
        var plan = requirePlanInProject(projectId, id);
        testPlanRepository.delete(plan);
        log.info("test_plan_deleted: id={} uid={}", plan.getId(), plan.getUid());
    }

    private TestPlan requirePlanInProject(UUID projectId, UUID id) {
        return testPlanRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Test plan not found: " + id));
    }
}
