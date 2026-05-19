package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunStepResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunStepResultRepository extends JpaRepository<TestRunStepResult, UUID> {

    @Query("SELECT s FROM TestRunStepResult s JOIN FETCH s.testCaseStep "
            + "WHERE s.testRunCaseResult.id = :caseResultId "
            + "ORDER BY s.snapshotOrder, s.id")
    List<TestRunStepResult> findByTestRunCaseResultIdOrderBySnapshotOrder(@Param("caseResultId") UUID caseResultId);

    Optional<TestRunStepResult> findByIdAndTestRunCaseResultId(UUID id, UUID testRunCaseResultId);

    List<TestRunStepResult> findByTestRunCaseResultId(UUID testRunCaseResultId);

    /** All step-result rows under a given run. Used by service-level cascade
     * delete so child rows are removed before their parent case results. */
    @Query("SELECT s FROM TestRunStepResult s WHERE s.testRunCaseResult.testRun.id = :runId")
    List<TestRunStepResult> findByTestRunId(@Param("runId") UUID runId);

    /** TC-009 / ADR-050 — existence probe used by
     * {@code TestCaseStepService.delete} so a parent step deletion that
     * would orphan run evidence raises a domain-aware
     * {@code ConflictException} instead of letting a database FK violation
     * surface as a generic conflict. */
    boolean existsByTestCaseStepId(UUID testCaseStepId);
}
