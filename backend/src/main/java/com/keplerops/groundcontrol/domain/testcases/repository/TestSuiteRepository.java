package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {

    Optional<TestSuite> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestSuite> findByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    @Query("SELECT s FROM TestSuite s WHERE s.project.id = :projectId ORDER BY s.createdAt DESC, s.id")
    List<TestSuite> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") UUID projectId);

    /**
     * Lock the suite row for the remainder of the current transaction so
     * static-membership mutations (add/remove/reorder) serialize per-suite
     * (codex pre-push cycle 2). Combined with the
     * {@code uq_test_suite_member_position} DB constraint, this rules out
     * the concurrent-append race where two callers both read the same
     * {@code size()} and insert different members at the same position.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TestSuite s WHERE s.id = :id AND s.project.id = :projectId")
    Optional<TestSuite> findByIdAndProjectIdForUpdate(@Param("id") UUID id, @Param("projectId") UUID projectId);
}
