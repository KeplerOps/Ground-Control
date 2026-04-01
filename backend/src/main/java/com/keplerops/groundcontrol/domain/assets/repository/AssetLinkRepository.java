package com.keplerops.groundcontrol.domain.assets.repository;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetLinkRepository extends JpaRepository<AssetLink, UUID> {

    @Query("SELECT l FROM AssetLink l JOIN FETCH l.asset WHERE l.asset.id = :assetId")
    List<AssetLink> findByAssetId(@Param("assetId") UUID assetId);

    @Query("SELECT l FROM AssetLink l JOIN FETCH l.asset WHERE l.asset.id = :assetId"
            + " AND l.targetType = :targetType")
    List<AssetLink> findByAssetIdAndTargetType(
            @Param("assetId") UUID assetId, @Param("targetType") AssetLinkTargetType targetType);

    boolean existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID assetId, AssetLinkTargetType targetType, String targetIdentifier, AssetLinkType linkType);

    @Query("SELECT l FROM AssetLink l JOIN FETCH l.asset WHERE l.targetType = :targetType"
            + " AND l.targetIdentifier = :targetIdentifier AND l.asset.project.id = :projectId")
    List<AssetLink> findByTargetTypeAndTargetIdentifierAndProjectId(
            @Param("targetType") AssetLinkTargetType targetType,
            @Param("targetIdentifier") String targetIdentifier,
            @Param("projectId") UUID projectId);
}
