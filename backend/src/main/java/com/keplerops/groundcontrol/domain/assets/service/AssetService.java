package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.AssetRelationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetService {

    private final OperationalAssetRepository assetRepository;
    private final AssetRelationRepository relationRepository;
    private final AssetLinkRepository linkRepository;
    private final ProjectRepository projectRepository;

    public AssetService(
            OperationalAssetRepository assetRepository,
            AssetRelationRepository relationRepository,
            AssetLinkRepository linkRepository,
            ProjectRepository projectRepository) {
        this.assetRepository = assetRepository;
        this.relationRepository = relationRepository;
        this.linkRepository = linkRepository;
        this.projectRepository = projectRepository;
    }

    public OperationalAsset create(CreateAssetCommand command) {
        var project = projectRepository
                .findById(command.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + command.projectId()));

        String normalizedUid = command.uid().toUpperCase(java.util.Locale.ROOT);
        if (assetRepository.existsByProjectIdAndUidIgnoreCase(command.projectId(), normalizedUid)) {
            throw new ConflictException("Asset with UID " + normalizedUid + " already exists in project");
        }

        var asset = new OperationalAsset(project, normalizedUid, command.name());
        if (command.description() != null) {
            asset.setDescription(command.description());
        }
        if (command.assetType() != null) {
            asset.setAssetType(command.assetType());
        }
        return assetRepository.save(asset);
    }

    public OperationalAsset update(UUID id, UpdateAssetCommand command) {
        var asset = getById(id);
        if (command.name() != null) {
            if (command.name().isBlank()) {
                throw new DomainValidationException("Asset name must not be blank");
            }
            asset.setName(command.name());
        }
        if (command.description() != null) {
            asset.setDescription(command.description());
        }
        if (command.assetType() != null) {
            asset.setAssetType(command.assetType());
        }
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public OperationalAsset getById(UUID id) {
        return assetRepository.findById(id).orElseThrow(() -> new NotFoundException("Asset not found: " + id));
    }

    @Transactional(readOnly = true)
    public OperationalAsset getByUid(UUID projectId, String uid) {
        return assetRepository
                .findByProjectIdAndUidIgnoreCase(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<OperationalAsset> listByProject(UUID projectId) {
        return assetRepository.findByProjectIdAndArchivedAtIsNull(projectId);
    }

    @Transactional(readOnly = true)
    public List<OperationalAsset> listByProjectAndType(UUID projectId, AssetType assetType) {
        return assetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(projectId, assetType);
    }

    public OperationalAsset archive(UUID id) {
        var asset = getById(id);
        asset.archive();
        return assetRepository.save(asset);
    }

    public void delete(UUID id) {
        var asset = getById(id);
        assetRepository.delete(asset);
    }

    public AssetRelation createRelation(UUID sourceId, UUID targetId, AssetRelationType relationType) {
        if (sourceId.equals(targetId)) {
            throw new DomainValidationException("An asset cannot relate to itself");
        }
        if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(sourceId, targetId, relationType)) {
            throw new ConflictException(
                    "Relation " + relationType + " already exists between " + sourceId + " and " + targetId);
        }
        var source = getById(sourceId);
        var target = getById(targetId);
        if (!source.getProject().getId().equals(target.getProject().getId())) {
            throw new DomainValidationException("Cannot create relation between assets in different projects");
        }
        var relation = new AssetRelation(source, target, relationType);
        return relationRepository.save(relation);
    }

    @Transactional(readOnly = true)
    public List<AssetRelation> getRelations(UUID assetId) {
        getById(assetId);
        var outgoing = relationRepository.findBySourceIdWithEntities(assetId);
        var incoming = relationRepository.findByTargetIdWithEntities(assetId);
        var combined = new ArrayList<AssetRelation>(outgoing);
        combined.addAll(incoming);
        return combined;
    }

    public void deleteRelation(UUID assetId, UUID relationId) {
        var relation = relationRepository
                .findById(relationId)
                .orElseThrow(() -> new NotFoundException("Relation not found: " + relationId));
        if (!relation.getSource().getId().equals(assetId)
                && !relation.getTarget().getId().equals(assetId)) {
            throw new NotFoundException("Relation " + relationId + " does not belong to asset " + assetId);
        }
        relationRepository.delete(relation);
    }

    // --- Asset Links (cross-entity linking) ---

    public AssetLink createLink(UUID assetId, CreateAssetLinkCommand command) {
        var asset = getById(assetId);
        if (linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                assetId, command.targetType(), command.targetIdentifier(), command.linkType())) {
            throw new ConflictException("Link already exists: " + command.linkType() + " -> " + command.targetType()
                    + ":" + command.targetIdentifier());
        }
        var link = new AssetLink(asset, command.targetType(), command.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }
        return linkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAsset(UUID assetId) {
        getById(assetId);
        return linkRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAssetByTargetType(UUID assetId, AssetLinkTargetType targetType) {
        getById(assetId);
        return linkRepository.findByAssetIdAndTargetType(assetId, targetType);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksByTarget(AssetLinkTargetType targetType, String targetIdentifier) {
        return linkRepository.findByTargetTypeAndTargetIdentifier(targetType, targetIdentifier);
    }

    public void deleteLink(UUID assetId, UUID linkId) {
        var link =
                linkRepository.findById(linkId).orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        if (!link.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to asset " + assetId);
        }
        linkRepository.delete(link);
    }
}
