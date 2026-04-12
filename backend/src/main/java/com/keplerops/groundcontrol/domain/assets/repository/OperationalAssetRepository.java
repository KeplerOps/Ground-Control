package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
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
}
