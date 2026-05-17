package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationalAssetRepository extends JpaRepository<OperationalAsset, UUID> {

    List<OperationalAsset> findByProjectIdAndArchivedAtIsNull(UUID projectId);

    List<OperationalAsset> findByProjectIdAndAssetTypeAndArchivedAtIsNull(UUID projectId, AssetType assetType);

    boolean existsByProjectIdAndUidIgnoreCase(UUID projectId, String uid);

    Optional<OperationalAsset> findByProjectIdAndUidIgnoreCase(UUID projectId, String uid);

    Optional<OperationalAsset> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    @Query("SELECT a.id FROM OperationalAsset a WHERE a.project.id = :projectId AND a.archivedAt IS NULL")
    List<UUID> findIdsByProjectIdAndArchivedAtIsNull(@Param("projectId") UUID projectId);

    /**
     * GC-M012 + GC-M011: single filtered project-scoped query for active
     * assets. Any combination of asset-type, owner, steward, environment,
     * criticality, scope-designation, and subtype predicates may be null;
     * nulls disable that predicate (IS NULL guard), so risk / control / audit
     * / reporting callers share one query surface instead of inventing
     * per-workflow lookups. Owner / steward comparison is case-insensitive to
     * match the free-form string shape; subtype is case-sensitive because the
     * subtype catalog (AssetSubtypeSchema) keys off the exact string.
     */
    @SuppressWarnings("java:S107") // JPA @Query requires each filter as an explicit @Param binding.
    @Query(
            """
            SELECT a FROM OperationalAsset a
            WHERE a.project.id = :projectId
              AND a.archivedAt IS NULL
              AND (:assetType IS NULL OR a.assetType = :assetType)
              AND (:owner IS NULL OR LOWER(a.owner) = LOWER(:owner))
              AND (:steward IS NULL OR LOWER(a.steward) = LOWER(:steward))
              AND (:environment IS NULL OR a.environment = :environment)
              AND (:criticality IS NULL OR a.criticality = :criticality)
              AND (:scopeDesignation IS NULL OR a.scopeDesignation = :scopeDesignation)
              AND (:subtype IS NULL OR a.subtype = :subtype)
            """)
    List<OperationalAsset> findByProjectIdAndArchivedAtIsNullAndFilters(
            @Param("projectId") UUID projectId,
            @Param("assetType") AssetType assetType,
            @Param("owner") String owner,
            @Param("steward") String steward,
            @Param("environment") AssetEnvironment environment,
            @Param("criticality") AssetCriticality criticality,
            @Param("scopeDesignation") AssetScope scopeDesignation,
            @Param("subtype") String subtype);
}
