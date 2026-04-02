package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.AssetExternalId;
import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetExternalIdRepository;
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
import java.time.Instant;
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
    private final AssetExternalIdRepository externalIdRepository;
    private final ProjectRepository projectRepository;

    public AssetService(
            OperationalAssetRepository assetRepository,
            AssetRelationRepository relationRepository,
            AssetLinkRepository linkRepository,
            AssetExternalIdRepository externalIdRepository,
            ProjectRepository projectRepository) {
        this.assetRepository = assetRepository;
        this.relationRepository = relationRepository;
        this.linkRepository = linkRepository;
        this.externalIdRepository = externalIdRepository;
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
        return createRelation(
                new CreateAssetRelationCommand(targetId, relationType, null, null, null, null, null), sourceId);
    }

    public AssetRelation createRelation(CreateAssetRelationCommand command, UUID sourceId) {
        if (sourceId.equals(command.targetId())) {
            throw new DomainValidationException("An asset cannot relate to itself");
        }
        if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                sourceId, command.targetId(), command.relationType())) {
            throw new ConflictException("Relation " + command.relationType() + " already exists between " + sourceId
                    + " and " + command.targetId());
        }
        var source = getById(sourceId);
        var target = getById(command.targetId());
        if (!source.getProject().getId().equals(target.getProject().getId())) {
            throw new DomainValidationException("Cannot create relation between assets in different projects");
        }
        var relation = new AssetRelation(source, target, command.relationType());
        applyRelationMetadata(
                relation,
                command.description(),
                command.sourceSystem(),
                command.externalSourceId(),
                command.collectedAt(),
                command.confidence());
        return relationRepository.save(relation);
    }

    public AssetRelation updateRelation(UUID assetId, UUID relationId, UpdateAssetRelationCommand command) {
        var relation = getRelationBelongingTo(assetId, relationId);
        applyRelationMetadata(
                relation,
                command.description(),
                command.sourceSystem(),
                command.externalSourceId(),
                command.collectedAt(),
                command.confidence());
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
        relationRepository.delete(getRelationBelongingTo(assetId, relationId));
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
    public List<AssetLink> getLinksByTarget(UUID projectId, AssetLinkTargetType targetType, String targetIdentifier) {
        return linkRepository.findByTargetTypeAndTargetIdentifierAndProjectId(targetType, targetIdentifier, projectId);
    }

    public void deleteLink(UUID assetId, UUID linkId) {
        var link =
                linkRepository.findById(linkId).orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        if (!link.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to asset " + assetId);
        }
        linkRepository.delete(link);
    }

    // --- External Identifiers (source provenance) ---

    public AssetExternalId createExternalId(UUID assetId, CreateAssetExternalIdCommand command) {
        var asset = getById(assetId);
        if (externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(
                assetId, command.sourceSystem(), command.sourceId())) {
            throw new ConflictException("External ID already exists: " + command.sourceSystem() + ":"
                    + command.sourceId() + " for asset " + assetId);
        }
        var extId = new AssetExternalId(asset, command.sourceSystem(), command.sourceId());
        applyProvenanceFields(extId, command.collectedAt(), command.confidence());
        return externalIdRepository.save(extId);
    }

    public AssetExternalId updateExternalId(UUID assetId, UUID extIdId, UpdateAssetExternalIdCommand command) {
        var extId = getExternalIdBelongingTo(assetId, extIdId);
        applyProvenanceFields(extId, command.collectedAt(), command.confidence());
        return externalIdRepository.save(extId);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIds(UUID assetId) {
        getById(assetId);
        return externalIdRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIdsBySource(UUID assetId, String sourceSystem) {
        getById(assetId);
        return externalIdRepository.findByAssetIdAndSourceSystem(assetId, sourceSystem);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> findByExternalId(UUID projectId, String sourceSystem, String sourceId) {
        return externalIdRepository.findBySourceSystemAndSourceIdAndProjectId(sourceSystem, sourceId, projectId);
    }

    public void deleteExternalId(UUID assetId, UUID extIdId) {
        externalIdRepository.delete(getExternalIdBelongingTo(assetId, extIdId));
    }

    private AssetExternalId getExternalIdBelongingTo(UUID assetId, UUID extIdId) {
        var extId = externalIdRepository
                .findByIdWithAsset(extIdId)
                .orElseThrow(() -> new NotFoundException("External ID not found: " + extIdId));
        if (!extId.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("External ID " + extIdId + " does not belong to asset " + assetId);
        }
        return extId;
    }

    private AssetRelation getRelationBelongingTo(UUID assetId, UUID relationId) {
        var relation = relationRepository
                .findByIdWithEntities(relationId)
                .orElseThrow(() -> new NotFoundException("Relation not found: " + relationId));
        if (!relation.getSource().getId().equals(assetId)
                && !relation.getTarget().getId().equals(assetId)) {
            throw new NotFoundException("Relation " + relationId + " does not belong to asset " + assetId);
        }
        return relation;
    }

    private void applyProvenanceFields(AssetExternalId extId, Instant collectedAt, String confidence) {
        if (collectedAt != null) {
            extId.setCollectedAt(collectedAt);
        }
        if (confidence != null) {
            extId.setConfidence(confidence);
        }
    }

    private void applyRelationMetadata(
            AssetRelation relation,
            String description,
            String sourceSystem,
            String externalSourceId,
            Instant collectedAt,
            String confidence) {
        if (description != null) {
            relation.setDescription(description);
        }
        if (sourceSystem != null) {
            relation.setSourceSystem(sourceSystem);
        }
        if (externalSourceId != null) {
            relation.setExternalSourceId(externalSourceId);
        }
        if (collectedAt != null) {
            relation.setCollectedAt(collectedAt);
        }
        if (confidence != null) {
            relation.setConfidence(confidence);
        }
    }
}
