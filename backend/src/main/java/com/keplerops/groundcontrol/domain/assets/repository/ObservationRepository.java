package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.id = :id")
    Optional<Observation> findByIdWithAsset(UUID id);

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset" + " WHERE o.id = :id AND o.asset.project.id = :projectId")
    Optional<Observation> findByIdWithAssetAndProjectId(UUID id, UUID projectId);

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId ORDER BY o.observedAt DESC")
    List<Observation> findByAssetId(UUID assetId);

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset"
            + " WHERE o.asset.project.id = :projectId ORDER BY o.observedAt DESC")
    List<Observation> findByProjectId(@Param("projectId") UUID projectId);

    @Query(
            "SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId AND o.category = :category ORDER BY o.observedAt DESC")
    List<Observation> findByAssetIdAndCategory(UUID assetId, ObservationCategory category);

    @Query(
            "SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId AND o.observationKey = :key ORDER BY o.observedAt DESC")
    List<Observation> findByAssetIdAndKey(UUID assetId, String key);

    @Query(
            "SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId AND o.category = :category AND o.observationKey = :key ORDER BY o.observedAt DESC")
    List<Observation> findByAssetIdAndCategoryAndKey(UUID assetId, ObservationCategory category, String key);

    boolean existsByAssetIdAndCategoryAndObservationKeyAndObservedAt(
            UUID assetId, ObservationCategory category, String observationKey, java.time.Instant observedAt);

    @Query("SELECT (COUNT(o) > 0) FROM Observation o WHERE o.id = :id AND o.asset.project.id = :projectId")
    boolean existsByIdAndProjectId(@Param("id") UUID id, @Param("projectId") UUID projectId);

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId "
            + "AND (o.expiresAt IS NULL OR o.expiresAt > :now) "
            + "AND o.observedAt = (SELECT MAX(o2.observedAt) FROM Observation o2 "
            + "WHERE o2.asset.id = :assetId AND o2.category = o.category AND o2.observationKey = o.observationKey "
            + "AND (o2.expiresAt IS NULL OR o2.expiresAt > :now)) "
            + "ORDER BY o.category, o.observationKey")
    List<Observation> findLatestByAssetId(UUID assetId, java.time.Instant now);

    /**
     * Time-travel-safe latest observation projection for one asset. Returns the
     * latest observation per (category, observationKey) whose {@code observedAt}
     * is {@code <= :asOf} and which is not expired as of {@code :asOf}. Unlike
     * {@link #findLatestByAssetId(UUID, java.time.Instant)} this never surfaces
     * observations from the future of the as-of point (GC-L007 finding #2).
     */
    @Query("SELECT o FROM Observation o JOIN FETCH o.asset WHERE o.asset.id = :assetId "
            + "AND o.observedAt <= :asOf "
            + "AND (o.expiresAt IS NULL OR o.expiresAt > :asOf) "
            + "AND o.observedAt = (SELECT MAX(o2.observedAt) FROM Observation o2 "
            + "WHERE o2.asset.id = :assetId AND o2.category = o.category AND o2.observationKey = o.observationKey "
            + "AND o2.observedAt <= :asOf "
            + "AND (o2.expiresAt IS NULL OR o2.expiresAt > :asOf)) "
            + "ORDER BY o.category, o.observationKey")
    List<Observation> findLatestByAssetIdAsOf(@Param("assetId") UUID assetId, @Param("asOf") java.time.Instant asOf);

    /**
     * Project-wide time-travel-safe latest observation projection. Returns the
     * latest observation per (assetId, category, observationKey) tuple whose
     * {@code observedAt} is {@code <= :asOf} and which is not expired as of
     * {@code :asOf}. The asset is fetch-joined so callers can group by asset
     * without per-row queries (GC-L007 finding #7, N+1).
     */
    @Query("SELECT o FROM Observation o JOIN FETCH o.asset "
            + "WHERE o.asset.project.id = :projectId "
            + "AND o.observedAt <= :asOf "
            + "AND (o.expiresAt IS NULL OR o.expiresAt > :asOf) "
            + "AND o.observedAt = (SELECT MAX(o2.observedAt) FROM Observation o2 "
            + "WHERE o2.asset.id = o.asset.id AND o2.category = o.category AND o2.observationKey = o.observationKey "
            + "AND o2.observedAt <= :asOf "
            + "AND (o2.expiresAt IS NULL OR o2.expiresAt > :asOf)) "
            + "ORDER BY o.asset.id, o.category, o.observationKey")
    List<Observation> findLatestByProjectIdAsOf(
            @Param("projectId") UUID projectId, @Param("asOf") java.time.Instant asOf);

    /**
     * All project observations whose {@code observedAt} is {@code <= :asOf}.
     * Used by services that need historical-as-of listings without filtering
     * out future rows in memory (GC-L007 finding #2).
     */
    @Query("SELECT o FROM Observation o JOIN FETCH o.asset "
            + "WHERE o.asset.project.id = :projectId AND o.observedAt <= :asOf "
            + "ORDER BY o.observedAt DESC")
    List<Observation> findByProjectIdAndObservedAtLessThanEqual(
            @Param("projectId") UUID projectId, @Param("asOf") java.time.Instant asOf);

    /**
     * All observations for an asset whose {@code observedAt} is {@code <= :asOf}.
     * Used by services that need historical-as-of listings without filtering
     * out future rows in memory (GC-L007 finding #2).
     */
    @Query("SELECT o FROM Observation o JOIN FETCH o.asset "
            + "WHERE o.asset.id = :assetId AND o.observedAt <= :asOf "
            + "ORDER BY o.observedAt DESC")
    List<Observation> findByAssetIdAndObservedAtLessThanEqual(
            @Param("assetId") UUID assetId, @Param("asOf") java.time.Instant asOf);

    @Query("SELECT o FROM Observation o JOIN FETCH o.asset" + " WHERE o.id IN :ids AND o.asset.project.id = :projectId")
    List<Observation> findAllByIdInAndProjectId(@Param("ids") List<UUID> ids, @Param("projectId") UUID projectId);
}
