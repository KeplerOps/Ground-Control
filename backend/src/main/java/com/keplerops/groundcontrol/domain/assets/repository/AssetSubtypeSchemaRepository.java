package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.AssetSubtypeSchema;
import com.keplerops.groundcontrol.domain.assets.state.AssetSubtypeSchemaStatus;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetSubtypeSchemaRepository extends JpaRepository<AssetSubtypeSchema, UUID> {

    Optional<AssetSubtypeSchema> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<AssetSubtypeSchema> findByProjectIdAndAssetTypeAndSubtypeAndStatus(
            UUID projectId, AssetType assetType, String subtype, AssetSubtypeSchemaStatus status);

    boolean existsByProjectIdAndAssetTypeAndSubtypeAndSchemaVersion(
            UUID projectId, AssetType assetType, String subtype, String schemaVersion);

    List<AssetSubtypeSchema> findByProjectIdAndAssetTypeAndSubtype(UUID projectId, AssetType assetType, String subtype);

    List<AssetSubtypeSchema> findByProjectIdAndAssetType(UUID projectId, AssetType assetType);

    List<AssetSubtypeSchema> findByProjectId(UUID projectId);
}
