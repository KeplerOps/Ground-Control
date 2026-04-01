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

    List<AssetLink> findByAssetId(UUID assetId);

    List<AssetLink> findByAssetIdAndTargetType(UUID assetId, AssetLinkTargetType targetType);

    boolean existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
            UUID assetId, AssetLinkTargetType targetType, String targetIdentifier, AssetLinkType linkType);

    @Query("SELECT l FROM AssetLink l JOIN FETCH l.asset WHERE l.targetType = :targetType"
            + " AND l.targetIdentifier = :targetIdentifier")
    List<AssetLink> findByTargetTypeAndTargetIdentifier(
            @Param("targetType") AssetLinkTargetType targetType, @Param("targetIdentifier") String targetIdentifier);
}
