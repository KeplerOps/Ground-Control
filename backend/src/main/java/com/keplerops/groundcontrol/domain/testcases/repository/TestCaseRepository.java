package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByProjectIdAndUid(UUID projectId, String uid);

    List<TestCase> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    // TC-005 / ADR-043 — Tie-breakers (createdAt, id) make the order
    // deterministic even when two siblings share a sort_order (e.g.
    // pre-V082 backfilled rows after a manual sort_order update, or
    // future write paths that accept caller-supplied sort_order).
    @Query("SELECT t FROM TestCase t WHERE t.project.id = :projectId "
            + "AND t.parentFolder.id = :folderId ORDER BY t.sortOrder, t.createdAt, t.id")
    List<TestCase> findByProjectIdAndParentFolderIdOrderBySortOrder(
            @Param("projectId") UUID projectId, @Param("folderId") UUID folderId);

    @Query("SELECT t FROM TestCase t WHERE t.project.id = :projectId "
            + "AND t.parentFolder IS NULL ORDER BY t.sortOrder, t.createdAt, t.id")
    List<TestCase> findRootByProjectIdOrderBySortOrder(@Param("projectId") UUID projectId);

    @Query("SELECT t FROM TestCase t WHERE t.project.id = :projectId " + "ORDER BY t.sortOrder, t.createdAt, t.id")
    List<TestCase> findAllByProjectIdOrderBySortOrder(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(t) FROM TestCase t WHERE t.parentFolder.id = :folderId")
    long countByParentFolderId(@Param("folderId") UUID folderId);
}
