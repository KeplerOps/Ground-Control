package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunCaseResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunCaseResultRepository extends JpaRepository<TestRunCaseResult, UUID> {

    @Query("SELECT r FROM TestRunCaseResult r JOIN FETCH r.testCase WHERE r.testRun.id = :runId "
            + "ORDER BY r.snapshotOrder, r.id")
    List<TestRunCaseResult> findByTestRunIdOrderBySnapshotOrder(@Param("runId") UUID runId);

    Optional<TestRunCaseResult> findByTestRunIdAndTestCaseId(UUID testRunId, UUID testCaseId);

    boolean existsByTestRunIdAndTestCaseId(UUID testRunId, UUID testCaseId);

    List<TestRunCaseResult> findByTestRunId(UUID testRunId);

    /**
     * TC-008 / ADR-049 — existence probe used by {@code TestCaseService.delete}
     * so a parent test-case deletion that would orphan run evidence raises a
     * domain-aware {@code ConflictException} instead of letting a database
     * FK violation surface as a generic conflict.
     */
    boolean existsByTestCaseId(UUID testCaseId);
}
