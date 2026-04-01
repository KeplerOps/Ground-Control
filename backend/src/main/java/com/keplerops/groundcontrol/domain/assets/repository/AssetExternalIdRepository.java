package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.AssetExternalId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetExternalIdRepository extends JpaRepository<AssetExternalId, UUID> {

    @Query("SELECT e FROM AssetExternalId e JOIN FETCH e.asset WHERE e.asset.id = :assetId")
    List<AssetExternalId> findByAssetId(UUID assetId);

    @Query(
            "SELECT e FROM AssetExternalId e JOIN FETCH e.asset WHERE e.asset.id = :assetId AND e.sourceSystem = :sourceSystem")
    List<AssetExternalId> findByAssetIdAndSourceSystem(UUID assetId, String sourceSystem);

    boolean existsByAssetIdAndSourceSystemAndSourceId(UUID assetId, String sourceSystem, String sourceId);

    @Query(
            "SELECT e FROM AssetExternalId e JOIN FETCH e.asset WHERE e.sourceSystem = :sourceSystem AND e.sourceId = :sourceId AND e.asset.project.id = :projectId")
    List<AssetExternalId> findBySourceSystemAndSourceIdAndProjectId(
            String sourceSystem, String sourceId, UUID projectId);
}
