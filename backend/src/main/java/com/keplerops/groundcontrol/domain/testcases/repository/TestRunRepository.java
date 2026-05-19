package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunRepository extends JpaRepository<TestRun, UUID> {

    Optional<TestRun> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestRun> findByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    @Query("SELECT r FROM TestRun r WHERE r.project.id = :projectId ORDER BY r.createdAt DESC, r.id")
    List<TestRun> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") UUID projectId);

    /**
     * TC-008 / ADR-049 — existence probes used by parent-aggregate delete
     * services. A non-zero result means the parent (test plan, test suite,
     * or test case) is referenced by at least one execution record, so the
     * parent must reject deletion with a domain-aware {@code ConflictException}
     * rather than letting a {@code DataIntegrityViolationException} bubble
     * out of the FK.
     */
    boolean existsByTestPlanId(UUID testPlanId);

    boolean existsByTestSuiteId(UUID testSuiteId);
}
