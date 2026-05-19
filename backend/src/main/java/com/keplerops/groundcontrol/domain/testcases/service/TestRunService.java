package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunCaseResult;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunStepResult;
import com.keplerops.groundcontrol.domain.testcases.model.TestRunTesterAssignment;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseStepRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestPlanRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunCaseResultRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunStepResultRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestRunTesterAssignmentRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestSuiteRepository;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-008 / ADR-049 — Application service for {@link TestRun}.
 *
 * <p>Owns CRUD, status transitions, tester management, and per-case
 * result updates. On create, the service resolves the run's
 * {@link TestSuite} via {@link TestSuiteService#resolveTestCases} and
 * snapshots the resulting cases as {@link TestRunCaseResult} rows: that
 * snapshot is the canonical membership of the run, and subsequent
 * mutations to the source suite never rewrite it.
 *
 * <p>Plan / suite / test-case project-scope is validated explicitly on
 * every cross-aggregate lookup; cross-project references surface as
 * {@code NotFoundException} (concealment) so a probing caller never
 * learns about an artifact's existence in a foreign project.
 */
@Service
@Transactional
public class TestRunService {

    private static final Logger log = LoggerFactory.getLogger(TestRunService.class);

    private final TestRunRepository testRunRepository;
    private final TestRunTesterAssignmentRepository testerAssignmentRepository;
    private final TestRunCaseResultRepository caseResultRepository;
    private final TestRunStepResultRepository stepResultRepository;
    private final TestCaseStepRepository testCaseStepRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestSuiteService testSuiteService;
    private final ProjectService projectService;

    public TestRunService(
            TestRunRepository testRunRepository,
            TestRunTesterAssignmentRepository testerAssignmentRepository,
            TestRunCaseResultRepository caseResultRepository,
            TestRunStepResultRepository stepResultRepository,
            TestCaseStepRepository testCaseStepRepository,
            TestPlanRepository testPlanRepository,
            TestSuiteRepository testSuiteRepository,
            TestSuiteService testSuiteService,
            ProjectService projectService) {
        this.testRunRepository = testRunRepository;
        this.testerAssignmentRepository = testerAssignmentRepository;
        this.caseResultRepository = caseResultRepository;
        this.stepResultRepository = stepResultRepository;
        this.testCaseStepRepository = testCaseStepRepository;
        this.testPlanRepository = testPlanRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testSuiteService = testSuiteService;
        this.projectService = projectService;
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public TestRun create(CreateTestRunCommand command) {
        var project = projectService.getById(command.projectId());
        if (testRunRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Test run with UID " + command.uid() + " already exists in this project");
        }
        TestPlan plan = requirePlanInProject(project.getId(), command.testPlanId());
        TestSuite suite = requireSuiteInProject(project.getId(), command.testSuiteId());

        var run = new TestRun(project, plan, suite, command.uid(), command.name());
        run.setEnvironment(command.environment());
        run.setVersion(command.version());
        run.setBuild(command.build());
        // Schedule endpoints validate against each other on set; seed in the
        // order start_at → end_at so a back-to-front pair fails fast.
        run.setStartAt(command.startAt());
        run.setEndAt(command.endAt());
        run = testRunRepository.save(run);

        // Snapshot the resolved test cases. The cap (500) is enforced by
        // TestSuiteService.resolveTestCases; one TestRunCaseResult row per
        // resolved case, status defaulting to NOT_RUN. snapshotOrder
        // preserves the resolver's deterministic order (author position for
        // STATIC suites; UID ordering otherwise) so the run-side read
        // contract replays the resolved-at-create sequence even after the
        // source suite is edited.
        List<TestCase> resolved = testSuiteService.resolveTestCases(project.getId(), suite.getId());
        int totalStepResults = 0;
        for (int i = 0; i < resolved.size(); i++) {
            TestCase tc = resolved.get(i);
            var resultRow = caseResultRepository.save(new TestRunCaseResult(run, tc, tc.getUid(), tc.getTitle(), i));
            // TC-009 / ADR-050 — Snapshot the authored steps so later edits
            // to TestCaseStep (or a renumbering) never rewrite this run's
            // historical evidence. Same transactional moment as the
            // case-result snapshot; cases with zero steps yield zero rows.
            List<TestCaseStep> steps = testCaseStepRepository.findByTestCaseIdOrderByStepNumberAsc(tc.getId());
            for (int j = 0; j < steps.size(); j++) {
                TestCaseStep step = steps.get(j);
                stepResultRepository.save(new TestRunStepResult(
                        resultRow, step, step.getStepNumber(), step.getAction(), step.getExpectedResult(), j));
            }
            totalStepResults += steps.size();
        }
        log.info(
                "test_run_created: uid={} project={} id={} plan_id={} suite_id={} snapshot_size={} step_snapshot_size={}",
                run.getUid(),
                project.getIdentifier(),
                run.getId(),
                plan.getId(),
                suite.getId(),
                resolved.size(),
                totalStepResults);
        return run;
    }

    @Transactional(readOnly = true)
    public TestRun getById(UUID projectId, UUID id) {
        return requireRunInProject(projectId, id);
    }

    @Transactional(readOnly = true)
    public TestRun getByUid(UUID projectId, String uid) {
        return testRunRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Test run not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<TestRun> listByProject(UUID projectId) {
        return testRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public TestRun update(UUID projectId, UUID id, UpdateTestRunCommand command) {
        var run = requireRunInProject(projectId, id);
        if (command.name() != null) {
            run.setName(command.name());
        }
        run.setEnvironment(resolveNullable(command.clearEnvironment(), command.environment(), run.getEnvironment()));
        run.setVersion(resolveNullable(command.clearVersion(), command.version(), run.getVersion()));
        run.setBuild(resolveNullable(command.clearBuild(), command.build(), run.getBuild()));
        applyScheduleUpdate(run, command);
        run = testRunRepository.save(run);
        log.info("test_run_updated: id={} uid={}", run.getId(), run.getUid());
        return run;
    }

    public TestRun transitionStatus(UUID projectId, UUID id, TestRunStatus newStatus) {
        var run = requireRunInProject(projectId, id);
        run.transitionStatus(newStatus);
        run = testRunRepository.save(run);
        log.info("test_run_status_changed: id={} uid={} status={}", run.getId(), run.getUid(), run.getStatus());
        return run;
    }

    public void delete(UUID projectId, UUID id) {
        var run = requireRunInProject(projectId, id);
        // Children load via list-and-deleteAll so the persistence context
        // sees the removal alongside the parent (same pattern as
        // TestSuiteMemberRepository.findByTestSuiteId). A bulk @Modifying
        // delete would leave stale instances pointing at the removed run.
        var assignments = testerAssignmentRepository.findByTestRunId(run.getId());
        if (!assignments.isEmpty()) {
            testerAssignmentRepository.deleteAll(assignments);
        }
        /* TC-009 / ADR-050. Step-result rows are FK-bound to case results,
         * so they have to leave the database before their parents.
         * Querying once at run scope avoids an N+1 per case-result. */
        var stepResults = stepResultRepository.findByTestRunId(run.getId());
        if (!stepResults.isEmpty()) {
            stepResultRepository.deleteAll(stepResults);
        }
        var results = caseResultRepository.findByTestRunId(run.getId());
        if (!results.isEmpty()) {
            caseResultRepository.deleteAll(results);
        }
        testRunRepository.delete(run);
        log.info("test_run_deleted: id={} uid={}", run.getId(), run.getUid());
    }

    // ------------------------------------------------------------------
    // Testers
    // ------------------------------------------------------------------

    public TestRunTesterAssignment addTester(UUID projectId, UUID runId, String testerName) {
        var run = requireRunInProject(projectId, runId);
        if (testerName == null || testerName.isBlank()) {
            throw new DomainValidationException(
                    "Tester name must not be blank", "invalid_test_run_tester_assignment", Map.of());
        }
        if (testerAssignmentRepository.existsByTestRunIdAndTesterName(run.getId(), testerName)) {
            // Concealment-by-default: the duplicate hint reveals only that the
            // assignment exists, never the raw tester name (the value is PII).
            throw new ConflictException("Tester is already assigned to this run");
        }
        // The TestRunTesterAssignment constructor enforces the path-safe
        // allow-list; the entity-level invariant is the source of truth so a
        // service path that bypasses the controller's @Valid still rejects
        // path-reserved characters consistently.
        var assignment = testerAssignmentRepository.save(new TestRunTesterAssignment(run, testerName));
        // Log a stable non-PII identifier — never the tester value itself.
        log.info("test_run_tester_added: run_id={} assignment_id={}", run.getId(), assignment.getId());
        return assignment;
    }

    public void removeTester(UUID projectId, UUID runId, String testerName) {
        var run = requireRunInProject(projectId, runId);
        var assignment = testerAssignmentRepository
                .findByTestRunIdAndTesterName(run.getId(), testerName)
                // Concealment-by-default: the not-found hint reveals only the
                // run id and the operation, never the raw tester name.
                .orElseThrow(() -> new NotFoundException("Tester assignment not found for run " + run.getId()));
        testerAssignmentRepository.delete(assignment);
        log.info("test_run_tester_removed: run_id={} assignment_id={}", run.getId(), assignment.getId());
    }

    @Transactional(readOnly = true)
    public List<TestRunTesterAssignment> listTesters(UUID projectId, UUID runId) {
        var run = requireRunInProject(projectId, runId);
        return testerAssignmentRepository.findByTestRunIdOrderByTesterName(run.getId());
    }

    // ------------------------------------------------------------------
    // Per-case results
    // ------------------------------------------------------------------

    public TestRunCaseResult updateResult(
            UUID projectId, UUID runId, UUID testCaseId, UpdateTestRunCaseResultCommand command) {
        var run = requireRunInProject(projectId, runId);
        // TC-009 / ADR-050 — Lifecycle guard. Once a run is COMPLETED,
        // ABORTED, or ARCHIVED, its execution record is closed and must not
        // be rewritten — the preflight names "terminal/archived runs cannot
        // be mutated" as a binding service invariant. This guard applies to
        // every evidence-mutation path (updateResult, updateStepResult,
        // updateCursor).
        requireMutableRun(run);
        var result = caseResultRepository
                .findByTestRunIdAndTestCaseId(run.getId(), testCaseId)
                .orElseThrow(
                        () -> new NotFoundException("Test case " + testCaseId + " is not part of run " + run.getId()));
        // Optional-status protocol: when the caller omits status (null), the
        // existing value is preserved so a notes-only autosave from the UI
        // never overwrites a status the tester has flipped concurrently.
        if (command.status() != null) {
            result.setStatus(command.status());
        }
        result.setNotes(resolveNullable(command.clearNotes(), command.notes(), result.getNotes()));
        result = caseResultRepository.save(result);
        log.info(
                "test_run_result_updated: run_id={} test_case_id={} status={}",
                run.getId(),
                testCaseId,
                result.getStatus());
        return result;
    }

    @Transactional(readOnly = true)
    public List<TestRunCaseResult> listResults(UUID projectId, UUID runId) {
        var run = requireRunInProject(projectId, runId);
        return caseResultRepository.findByTestRunIdOrderBySnapshotOrder(run.getId());
    }

    // ------------------------------------------------------------------
    // Per-step results (TC-009 / ADR-050)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TestRunStepResult> listStepResults(UUID projectId, UUID runId, UUID caseResultId) {
        var run = requireRunInProject(projectId, runId);
        var caseResult = requireCaseResultInRun(run, caseResultId);
        return stepResultRepository.findByTestRunCaseResultIdOrderBySnapshotOrder(caseResult.getId());
    }

    public TestRunStepResult updateStepResult(
            UUID projectId, UUID runId, UUID caseResultId, UUID stepResultId, UpdateTestRunStepResultCommand command) {
        var run = requireRunInProject(projectId, runId);
        requireMutableRun(run);
        var caseResult = requireCaseResultInRun(run, caseResultId);
        var stepResult = requireStepResultInCaseResult(caseResult, stepResultId);
        // Optional-status protocol: null preserves the existing value so a
        // comment-only autosave can't revert a concurrent status flip.
        if (command.status() != null) {
            stepResult.setStatus(command.status());
        }
        stepResult.setComment(resolveNullable(command.clearComment(), command.comment(), stepResult.getComment()));
        // Execution timestamp resolution:
        //   1. clearExecutedAt=true   → null (explicit reset).
        //   2. executedAt supplied    → use it.
        //   3. status is non-NOT_RUN AND existing is null AND no explicit
        //      value supplied → default to Instant.now() server-side so
        //      every executed step carries a timestamp regardless of which
        //      client (SPA, MCP, raw REST) drove the update — TC-009
        //      requires execution timestamps for every observed step.
        //   4. otherwise preserve existing.
        if (command.clearExecutedAt()) {
            stepResult.setExecutedAt(null);
        } else if (command.executedAt() != null) {
            stepResult.setExecutedAt(command.executedAt());
        } else if (stepResult.getStatus() != TestRunCaseResultStatus.NOT_RUN && stepResult.getExecutedAt() == null) {
            stepResult.setExecutedAt(Instant.now());
        }
        stepResult = stepResultRepository.save(stepResult);
        // Comment text is user evidence and may carry PII; log identity and
        // status only, never the raw comment body.
        log.info(
                "test_run_step_result_updated: run_id={} case_result_id={} step_result_id={} status={}",
                run.getId(),
                caseResult.getId(),
                stepResult.getId(),
                stepResult.getStatus());
        return stepResult;
    }

    public TestRun updateCursor(UUID projectId, UUID runId, UpdateTestRunCursorCommand command) {
        var run = requireRunInProject(projectId, runId);
        requireMutableRun(run);
        if (command.clearCursor()) {
            run.setCurrentCaseResultId(null);
            run.setCurrentStepResultId(null);
        } else {
            TestRunCaseResult caseResult = null;
            if (command.currentCaseResultId() != null) {
                caseResult = requireCaseResultInRun(run, command.currentCaseResultId());
            }
            if (command.currentStepResultId() != null) {
                if (caseResult == null) {
                    throw new DomainValidationException(
                            "current_case_result_id must be set when current_step_result_id is set",
                            "invalid_test_run_cursor",
                            Map.of());
                }
                requireStepResultInCaseResult(caseResult, command.currentStepResultId());
            }
            run.setCurrentCaseResultId(command.currentCaseResultId());
            run.setCurrentStepResultId(command.currentStepResultId());
        }
        run = testRunRepository.save(run);
        log.info(
                "test_run_cursor_updated: run_id={} current_case_result_id={} current_step_result_id={}",
                run.getId(),
                run.getCurrentCaseResultId(),
                run.getCurrentStepResultId());
        return run;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Reconcile the target start_at / end_at pair once and apply atomically,
     * mirroring {@code TestPlanService.applyScheduleUpdate}. The per-field
     * setters compare against the stored counterpart, so a whole-window
     * shift would otherwise be rejected when the new start is compared to
     * the still-old end. Clearing both before reassigning lets the setter
     * invariants see a clean state.
     */
    private static void applyScheduleUpdate(TestRun run, UpdateTestRunCommand command) {
        Instant targetStart = resolveNullable(command.clearStartAt(), command.startAt(), run.getStartAt());
        Instant targetEnd = resolveNullable(command.clearEndAt(), command.endAt(), run.getEndAt());
        if (targetStart != null && targetEnd != null && targetStart.isAfter(targetEnd)) {
            throw new DomainValidationException(
                    "end_at must be on or after start_at",
                    "invalid_test_run_window",
                    Map.of("start_at", targetStart.toString(), "end_at", targetEnd.toString()));
        }
        run.setStartAt(null);
        run.setEndAt(null);
        run.setStartAt(targetStart);
        run.setEndAt(targetEnd);
    }

    private static <T> T resolveNullable(boolean clear, T incoming, T current) {
        if (clear) {
            return null;
        }
        return incoming != null ? incoming : current;
    }

    private TestRun requireRunInProject(UUID projectId, UUID id) {
        return testRunRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Test run not found: " + id));
    }

    /**
     * TC-009 / ADR-050 — Reject execution-evidence and cursor writes on
     * runs whose lifecycle has closed. Once a run is COMPLETED, ABORTED, or
     * ARCHIVED, its execution record is the historical artifact of that pass
     * and must not be rewritten. The preflight names this as a binding
     * service-layer invariant; without the guard, an authenticated project
     * user can PUT step-result evidence on a closed run after the fact.
     */
    private static void requireMutableRun(TestRun run) {
        var status = run.getStatus();
        if (status == TestRunStatus.COMPLETED || status == TestRunStatus.ABORTED || status == TestRunStatus.ARCHIVED) {
            throw new DomainValidationException(
                    "Test run " + run.getId() + " is " + status + " and cannot be mutated",
                    "invalid_test_run_state",
                    Map.of("status", status.name()));
        }
    }

    private TestPlan requirePlanInProject(UUID projectId, UUID planId) {
        if (planId == null) {
            throw new DomainValidationException("test_plan_id must not be null", "invalid_test_run", Map.of());
        }
        return testPlanRepository
                .findByIdAndProjectId(planId, projectId)
                .orElseThrow(() -> new NotFoundException("Test plan not found: " + planId));
    }

    private TestSuite requireSuiteInProject(UUID projectId, UUID suiteId) {
        if (suiteId == null) {
            throw new DomainValidationException("test_suite_id must not be null", "invalid_test_run", Map.of());
        }
        return testSuiteRepository
                .findByIdAndProjectId(suiteId, projectId)
                .orElseThrow(() -> new NotFoundException("Test suite not found: " + suiteId));
    }

    private TestRunCaseResult requireCaseResultInRun(TestRun run, UUID caseResultId) {
        if (caseResultId == null) {
            throw new DomainValidationException(
                    "case_result_id must not be null", "invalid_test_run_case_result", Map.of());
        }
        var caseResult = caseResultRepository
                .findById(caseResultId)
                .orElseThrow(() ->
                        new NotFoundException("Case result " + caseResultId + " is not part of run " + run.getId()));
        // Concealment: a case result that belongs to a different run is
        // indistinguishable from a not-found one.
        if (!caseResult.getTestRun().getId().equals(run.getId())) {
            throw new NotFoundException("Case result " + caseResultId + " is not part of run " + run.getId());
        }
        return caseResult;
    }

    private TestRunStepResult requireStepResultInCaseResult(TestRunCaseResult caseResult, UUID stepResultId) {
        if (stepResultId == null) {
            throw new DomainValidationException(
                    "step_result_id must not be null", "invalid_test_run_step_result", Map.of());
        }
        return stepResultRepository
                .findByIdAndTestRunCaseResultId(stepResultId, caseResult.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Step result " + stepResultId + " is not part of case result " + caseResult.getId()));
    }
}
