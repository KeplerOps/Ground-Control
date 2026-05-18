package com.keplerops.groundcontrol.domain.testcases.repository;

import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID>, JpaSpecificationExecutor<TestCase> {

    boolean existsByProjectIdAndUid(UUID projectId, String uid);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<TestCase> findByProjectIdAndUid(UUID projectId, String uid);

    List<TestCase> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * TC-007 / ADR-047 — REQUIREMENTS_BASED test-suite resolution. Returns
     * the test cases in {@code projectId} whose UID is in {@code uids};
     * unmatched UIDs are silently dropped (the linkage table may carry an
     * artifact identifier that no longer maps to a live test case). One
     * batched lookup keeps the resolve cost O(1) round trips instead of
     * one per source requirement / per linked artifact.
     */
    @Query("SELECT t FROM TestCase t WHERE t.project.id = :projectId AND t.uid IN :uids")
    List<TestCase> findByProjectIdAndUidIn(@Param("projectId") UUID projectId, @Param("uids") Collection<String> uids);

    /**
     * TC-007 / ADR-047 — REQUIREMENTS_BASED resolve in a single
     * filter+join+sort+cap query (codex pre-push cycle 3). Returns up to
     * {@code pageable.size} live test cases in {@code projectId} that are
     * linked to one of {@code requirementIds} via a TraceabilityLink with
     * the requested {@code linkType} / {@code artifactType}. The join is
     * the live-filter: stale artifact identifiers whose UID no longer
     * names a project test case are never counted against the cap, so
     * the cap consumes only live, project-scoped, deterministically
     * ordered results.
     */
    @Query("SELECT t FROM TestCase t WHERE t.project.id = :projectId AND t.uid IN ("
            + "  SELECT DISTINCT l.artifactIdentifier FROM TraceabilityLink l"
            + "  WHERE l.requirement.id IN :requirementIds"
            + "  AND l.linkType = :linkType"
            + "  AND l.artifactType = :artifactType"
            + "  AND l.artifactIdentifier IS NOT NULL) ORDER BY t.uid")
    List<TestCase> findLinkedTestCasesForRequirements(
            @Param("projectId") UUID projectId,
            @Param("requirementIds") Collection<UUID> requirementIds,
            @Param("linkType") LinkType linkType,
            @Param("artifactType") ArtifactType artifactType,
            Pageable pageable);

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
