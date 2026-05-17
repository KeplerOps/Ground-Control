package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseFolderRepository extends JpaRepository<TestCaseFolder, UUID> {

    Optional<TestCaseFolder> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("SELECT f FROM TestCaseFolder f WHERE f.project.id = :projectId "
            + "ORDER BY f.sortOrder, f.createdAt, f.id")
    List<TestCaseFolder> findByProjectIdOrderBySortOrder(@Param("projectId") UUID projectId);

    // TC-005 / ADR-043 — Tie-breakers (createdAt, id) keep sibling order
    // deterministic when two folders share a sort_order.
    @Query("SELECT f FROM TestCaseFolder f WHERE f.project.id = :projectId "
            + "AND f.parent.id = :parentId ORDER BY f.sortOrder, f.createdAt, f.id")
    List<TestCaseFolder> findByProjectIdAndParentIdOrderBySortOrder(
            @Param("projectId") UUID projectId, @Param("parentId") UUID parentId);

    @Query("SELECT f FROM TestCaseFolder f WHERE f.project.id = :projectId "
            + "AND f.parent IS NULL ORDER BY f.sortOrder, f.createdAt, f.id")
    List<TestCaseFolder> findRootByProjectIdOrderBySortOrder(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(f) > 0 FROM TestCaseFolder f WHERE f.project.id = :projectId "
            + "AND f.parent.id = :parentId AND f.title = :title")
    boolean existsByProjectIdAndParentIdAndTitle(
            @Param("projectId") UUID projectId, @Param("parentId") UUID parentId, @Param("title") String title);

    @Query("SELECT COUNT(f) > 0 FROM TestCaseFolder f WHERE f.project.id = :projectId "
            + "AND f.parent IS NULL AND f.title = :title")
    boolean existsRootByProjectIdAndTitle(@Param("projectId") UUID projectId, @Param("title") String title);

    @Query("SELECT COUNT(f) FROM TestCaseFolder f WHERE f.parent.id = :parentId")
    long countByParentId(@Param("parentId") UUID parentId);
}
