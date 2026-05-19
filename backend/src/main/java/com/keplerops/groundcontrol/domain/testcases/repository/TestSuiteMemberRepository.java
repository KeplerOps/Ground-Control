package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteMemberRepository extends JpaRepository<TestSuiteMember, UUID> {

    @Query("SELECT m FROM TestSuiteMember m JOIN FETCH m.testCase tc "
            + "WHERE m.testSuite.id = :suiteId ORDER BY m.position, m.id")
    List<TestSuiteMember> findByTestSuiteIdOrderByPosition(@Param("suiteId") UUID suiteId);

    /**
     * Pageable variant used by the resolve path so the 500-result cap is
     * enforced at the database layer (codex pre-push cycle 1 F3) — the
     * non-paged variant above remains the source of truth for membership
     * mutations that must see the full list to keep positions contiguous.
     */
    @Query("SELECT m FROM TestSuiteMember m JOIN FETCH m.testCase tc "
            + "WHERE m.testSuite.id = :suiteId ORDER BY m.position, m.id")
    List<TestSuiteMember> findByTestSuiteIdOrderByPosition(@Param("suiteId") UUID suiteId, Pageable pageable);

    boolean existsByTestSuiteIdAndTestCaseId(UUID testSuiteId, UUID testCaseId);

    Optional<TestSuiteMember> findByTestSuiteIdAndTestCaseId(UUID testSuiteId, UUID testCaseId);

    long countByTestSuiteId(UUID testSuiteId);

    /**
     * Fetch the bare-rows variant used by the service's delete cascade. We
     * load the entities into the persistence context so a subsequent
     * {@link #deleteAll} cleans them out of the PC alongside the database
     * row — a bulk {@code @Modifying} DELETE would leave stale instances in
     * the context that point at the now-removed parent suite, which would
     * trip Hibernate's TransientObjectException when the parent is then
     * deleted in the same transaction.
     */
    List<TestSuiteMember> findByTestSuiteId(UUID testSuiteId);
}
