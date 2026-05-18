package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.repository.TestPlanRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunRepository;
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
    private final TestRunRepository testRunRepository;
    private final ProjectService projectService;

    public TestPlanService(
            TestPlanRepository testPlanRepository, TestRunRepository testRunRepository, ProjectService projectService) {
        this.testPlanRepository = testPlanRepository;
        this.testRunRepository = testRunRepository;
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
        plan.setDescription(resolveNullable(command.clearDescription(), command.description(), plan.getDescription()));
        plan.setProduct(resolveNullable(command.clearProduct(), command.product(), plan.getProduct()));
        plan.setVersion(resolveNullable(command.clearVersion(), command.version(), plan.getVersion()));
        plan.setBuild(resolveNullable(command.clearBuild(), command.build(), plan.getBuild()));
        applyScheduleUpdate(plan, command);
        plan = testPlanRepository.save(plan);
        log.info("test_plan_updated: id={} uid={}", plan.getId(), plan.getUid());
        return plan;
    }

    /**
     * Reconcile the target start/end pair once and apply it atomically. The
     * entity setters each compare against the stored counterpart, so setting
     * them one at a time would reject a valid whole-window shift (codex
     * pre-push cycle 1 class finding): existing 6/1–6/30, new 7/1–7/31
     * would trip when the new start (7/1) is compared to the still-old
     * end (6/30). Computing the final pair from current state + clear flags
     * + new values, validating once, then clearing and reassigning lets
     * the per-field setters see a clean state.
     */
    private static void applyScheduleUpdate(TestPlan plan, UpdateTestPlanCommand command) {
        LocalDate targetStart = resolveNullable(command.clearStartDate(), command.startDate(), plan.getStartDate());
        LocalDate targetEnd = resolveNullable(command.clearEndDate(), command.endDate(), plan.getEndDate());
        if (targetStart != null && targetEnd != null && targetStart.isAfter(targetEnd)) {
            throw new DomainValidationException(
                    "End date must be on or after start date",
                    "invalid_test_plan_schedule",
                    Map.of("start", targetStart.toString(), "end", targetEnd.toString()));
        }
        // Clear both before reassigning so the setters' transient-pair
        // checks see (null, null) instead of stale state.
        plan.setStartDate(null);
        plan.setEndDate(null);
        plan.setStartDate(targetStart);
        plan.setEndDate(targetEnd);
    }

    /**
     * Apply the partial-update contract to a nullable field: {@code clear=true}
     * wins (wipes to null), otherwise a non-null {@code incoming} value
     * replaces the current state, otherwise the current state is preserved.
     */
    private static <T> T resolveNullable(boolean clear, T incoming, T current) {
        if (clear) {
            return null;
        }
        return incoming != null ? incoming : current;
    }

    public TestPlan transitionStatus(UUID projectId, UUID id, TestPlanStatus newStatus) {
        var plan = requirePlanInProject(projectId, id);
        plan.transitionStatus(newStatus);
        plan = testPlanRepository.save(plan);
        log.info("test_plan_status_changed: id={} uid={} status={}", plan.getId(), plan.getUid(), plan.getStatus());
        return plan;
    }

    /**
     * Delete a test plan after ensuring no test runs reference it.
     *
     * <p>Per TC-008 / ADR-049, a {@code TestRun} carries a non-null FK to the
     * driving plan. Without this guard, callers would receive a late
     * persistence-layer integrity violation translated to a generic conflict
     * envelope; the explicit check raises a domain-aware
     * {@link ConflictException} naming the plan UID and the resolution.
     */
    public void delete(UUID projectId, UUID id) {
        var plan = requirePlanInProject(projectId, id);
        if (testRunRepository.existsByTestPlanId(plan.getId())) {
            throw new ConflictException(
                    "Test plan " + plan.getUid() + " has associated test runs; archive or delete those first");
        }
        testPlanRepository.delete(plan);
        log.info("test_plan_deleted: id={} uid={}", plan.getId(), plan.getUid());
    }

    private TestPlan requirePlanInProject(UUID projectId, UUID id) {
        return testPlanRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Test plan not found: " + id));
    }
}
