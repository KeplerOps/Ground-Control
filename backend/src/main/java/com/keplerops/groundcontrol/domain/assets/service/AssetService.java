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
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
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
    private final GraphTargetResolverService graphTargetResolverService;

    public AssetService(
            OperationalAssetRepository assetRepository,
            AssetRelationRepository relationRepository,
            AssetLinkRepository linkRepository,
            AssetExternalIdRepository externalIdRepository,
            ProjectRepository projectRepository,
            GraphTargetResolverService graphTargetResolverService) {
        this.assetRepository = assetRepository;
        this.relationRepository = relationRepository;
        this.linkRepository = linkRepository;
        this.externalIdRepository = externalIdRepository;
        this.projectRepository = projectRepository;
        this.graphTargetResolverService = graphTargetResolverService;
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

    public OperationalAsset update(UUID projectId, UUID id, UpdateAssetCommand command) {
        var asset = getById(projectId, id);
        applyAssetUpdates(asset, command);
        return assetRepository.save(asset);
    }

    @Deprecated(forRemoval = false)
    public OperationalAsset update(UUID id, UpdateAssetCommand command) {
        var asset = assetRepository.findById(id).orElseThrow(() -> new NotFoundException("Asset not found: " + id));
        applyAssetUpdates(asset, command);
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public OperationalAsset getById(UUID projectId, UUID id) {
        return assetRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + id));
    }

    @Deprecated(forRemoval = false)
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

    public OperationalAsset archive(UUID projectId, UUID id) {
        var asset = getById(projectId, id);
        asset.archive();
        return assetRepository.save(asset);
    }

    @Deprecated(forRemoval = false)
    public OperationalAsset archive(UUID id) {
        var asset = getById(id);
        asset.archive();
        return assetRepository.save(asset);
    }

    public void delete(UUID projectId, UUID id) {
        var asset = getById(projectId, id);
        assetRepository.delete(asset);
    }

    @Deprecated(forRemoval = false)
    public void delete(UUID id) {
        assetRepository.delete(getById(id));
    }

    public AssetRelation createRelation(UUID projectId, UUID sourceId, UUID targetId, AssetRelationType relationType) {
        return createRelation(
                projectId,
                new CreateAssetRelationCommand(targetId, relationType, null, null, null, null, null),
                sourceId);
    }

    @Deprecated(forRemoval = false)
    public AssetRelation createRelation(UUID sourceId, UUID targetId, AssetRelationType relationType) {
        return createRelation(
                new CreateAssetRelationCommand(targetId, relationType, null, null, null, null, null), sourceId);
    }

    public AssetRelation createRelation(UUID projectId, CreateAssetRelationCommand command, UUID sourceId) {
        if (sourceId.equals(command.targetId())) {
            throw new DomainValidationException("An asset cannot relate to itself");
        }
        if (relationRepository.existsBySourceIdAndTargetIdAndRelationType(
                sourceId, command.targetId(), command.relationType())) {
            throw new ConflictException("Relation " + command.relationType() + " already exists between " + sourceId
                    + " and " + command.targetId());
        }
        var source = getById(projectId, sourceId);
        var target = getById(projectId, command.targetId());
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

    @Deprecated(forRemoval = false)
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
        validateSameProject(source, target);
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

    public AssetRelation updateRelation(
            UUID projectId, UUID assetId, UUID relationId, UpdateAssetRelationCommand command) {
        var relation = getRelationBelongingTo(projectId, assetId, relationId);
        applyRelationMetadata(
                relation,
                command.description(),
                command.sourceSystem(),
                command.externalSourceId(),
                command.collectedAt(),
                command.confidence());
        return relationRepository.save(relation);
    }

    @Deprecated(forRemoval = false)
    public AssetRelation updateRelation(UUID assetId, UUID relationId, UpdateAssetRelationCommand command) {
        var relation = getLegacyRelationBelongingTo(assetId, relationId);
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
    public List<AssetRelation> getRelations(UUID projectId, UUID assetId) {
        getById(projectId, assetId);
        var outgoing = relationRepository.findBySourceIdWithEntities(assetId);
        var incoming = relationRepository.findByTargetIdWithEntities(assetId);
        var combined = new ArrayList<AssetRelation>(outgoing);
        combined.addAll(incoming);
        return combined;
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<AssetRelation> getRelations(UUID assetId) {
        getById(assetId);
        var outgoing = relationRepository.findBySourceIdWithEntities(assetId);
        var incoming = relationRepository.findByTargetIdWithEntities(assetId);
        var combined = new ArrayList<AssetRelation>(outgoing);
        combined.addAll(incoming);
        return combined;
    }

    public void deleteRelation(UUID projectId, UUID assetId, UUID relationId) {
        relationRepository.delete(getRelationBelongingTo(projectId, assetId, relationId));
    }

    @Deprecated(forRemoval = false)
    public void deleteRelation(UUID assetId, UUID relationId) {
        relationRepository.delete(getLegacyRelationBelongingTo(assetId, relationId));
    }

    // --- Asset Links (cross-entity linking) ---

    public AssetLink createLink(UUID projectId, UUID assetId, CreateAssetLinkCommand command) {
        var asset = getById(projectId, assetId);
        var target = graphTargetResolverService.validateAssetTarget(
                projectId, command.targetType(), command.targetEntityId(), command.targetIdentifier());
        boolean exists = target.internal()
                ? linkRepository.existsByAssetIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        assetId, command.targetType(), target.targetEntityId(), command.linkType())
                : linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        assetId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Link already exists: " + command.linkType() + " -> " + command.targetType()
                    + ":" + (target.internal() ? target.targetEntityId() : target.targetIdentifier()));
        }
        var link = new AssetLink(
                asset, command.targetType(), target.targetEntityId(), target.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }
        return linkRepository.save(link);
    }

    @Deprecated(forRemoval = false)
    public AssetLink createLink(UUID assetId, CreateAssetLinkCommand command) {
        var asset = getById(assetId);
        var target = graphTargetResolverService.validateAssetTarget(
                asset.getProject().getId(), command.targetType(), command.targetEntityId(), command.targetIdentifier());
        boolean exists = target.internal()
                ? linkRepository.existsByAssetIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        assetId, command.targetType(), target.targetEntityId(), command.linkType())
                : linkRepository.existsByAssetIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        assetId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Link already exists: " + command.linkType() + " -> " + command.targetType()
                    + ":" + (target.internal() ? target.targetEntityId() : target.targetIdentifier()));
        }
        var link = new AssetLink(
                asset, command.targetType(), target.targetEntityId(), target.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }
        return linkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAsset(UUID projectId, UUID assetId) {
        getById(projectId, assetId);
        return linkRepository.findByAssetId(assetId);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAsset(UUID assetId) {
        getById(assetId);
        return linkRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAssetByTargetType(UUID projectId, UUID assetId, AssetLinkTargetType targetType) {
        getById(projectId, assetId);
        return linkRepository.findByAssetIdAndTargetType(assetId, targetType);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<AssetLink> getLinksForAssetByTargetType(UUID assetId, AssetLinkTargetType targetType) {
        getById(assetId);
        return linkRepository.findByAssetIdAndTargetType(assetId, targetType);
    }

    @Transactional(readOnly = true)
    public List<AssetLink> getLinksByTarget(
            UUID projectId, AssetLinkTargetType targetType, UUID targetEntityId, String targetIdentifier) {
        if (targetEntityId != null) {
            return linkRepository.findByTargetTypeAndTargetEntityIdAndProjectId(targetType, targetEntityId, projectId);
        }
        return linkRepository.findByTargetTypeAndTargetIdentifierAndProjectId(targetType, targetIdentifier, projectId);
    }

    public void deleteLink(UUID projectId, UUID assetId, UUID linkId) {
        var link = linkRepository
                .findByIdWithAssetAndProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        if (!link.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to asset " + assetId);
        }
        linkRepository.delete(link);
    }

    @Deprecated(forRemoval = false)
    public void deleteLink(UUID assetId, UUID linkId) {
        var link =
                linkRepository.findById(linkId).orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        if (!link.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to asset " + assetId);
        }
        linkRepository.delete(link);
    }

    // --- External Identifiers (source provenance) ---

    public AssetExternalId createExternalId(UUID projectId, UUID assetId, CreateAssetExternalIdCommand command) {
        var asset = getById(projectId, assetId);
        if (externalIdRepository.existsByAssetIdAndSourceSystemAndSourceId(
                assetId, command.sourceSystem(), command.sourceId())) {
            throw new ConflictException("External ID already exists: " + command.sourceSystem() + ":"
                    + command.sourceId() + " for asset " + assetId);
        }
        var extId = new AssetExternalId(asset, command.sourceSystem(), command.sourceId());
        applyProvenanceFields(extId, command.collectedAt(), command.confidence());
        return externalIdRepository.save(extId);
    }

    @Deprecated(forRemoval = false)
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

    public AssetExternalId updateExternalId(
            UUID projectId, UUID assetId, UUID extIdId, UpdateAssetExternalIdCommand command) {
        var extId = getExternalIdBelongingTo(projectId, assetId, extIdId);
        applyProvenanceFields(extId, command.collectedAt(), command.confidence());
        return externalIdRepository.save(extId);
    }

    @Deprecated(forRemoval = false)
    public AssetExternalId updateExternalId(UUID assetId, UUID extIdId, UpdateAssetExternalIdCommand command) {
        var extId = getLegacyExternalIdBelongingTo(assetId, extIdId);
        applyProvenanceFields(extId, command.collectedAt(), command.confidence());
        return externalIdRepository.save(extId);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIds(UUID projectId, UUID assetId) {
        getById(projectId, assetId);
        return externalIdRepository.findByAssetId(assetId);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIds(UUID assetId) {
        getById(assetId);
        return externalIdRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIdsBySource(UUID projectId, UUID assetId, String sourceSystem) {
        getById(projectId, assetId);
        return externalIdRepository.findByAssetIdAndSourceSystem(assetId, sourceSystem);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<AssetExternalId> getExternalIdsBySource(UUID assetId, String sourceSystem) {
        getById(assetId);
        return externalIdRepository.findByAssetIdAndSourceSystem(assetId, sourceSystem);
    }

    @Transactional(readOnly = true)
    public List<AssetExternalId> findByExternalId(UUID projectId, String sourceSystem, String sourceId) {
        return externalIdRepository.findBySourceSystemAndSourceIdAndProjectId(sourceSystem, sourceId, projectId);
    }

    public void deleteExternalId(UUID projectId, UUID assetId, UUID extIdId) {
        externalIdRepository.delete(getExternalIdBelongingTo(projectId, assetId, extIdId));
    }

    @Deprecated(forRemoval = false)
    public void deleteExternalId(UUID assetId, UUID extIdId) {
        externalIdRepository.delete(getLegacyExternalIdBelongingTo(assetId, extIdId));
    }

    private AssetExternalId getExternalIdBelongingTo(UUID projectId, UUID assetId, UUID extIdId) {
        var extId = externalIdRepository
                .findByIdWithAssetAndProjectId(extIdId, projectId)
                .orElseThrow(() -> new NotFoundException("External ID not found: " + extIdId));
        if (!extId.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("External ID " + extIdId + " does not belong to asset " + assetId);
        }
        return extId;
    }

    private AssetExternalId getLegacyExternalIdBelongingTo(UUID assetId, UUID extIdId) {
        var extId = externalIdRepository
                .findByIdWithAsset(extIdId)
                .orElseThrow(() -> new NotFoundException("External ID not found: " + extIdId));
        if (!extId.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("External ID " + extIdId + " does not belong to asset " + assetId);
        }
        return extId;
    }

    private AssetRelation getRelationBelongingTo(UUID projectId, UUID assetId, UUID relationId) {
        var relation = relationRepository
                .findByIdWithEntitiesAndProjectId(relationId, projectId)
                .orElseThrow(() -> new NotFoundException("Relation not found: " + relationId));
        if (!relation.getSource().getId().equals(assetId)
                && !relation.getTarget().getId().equals(assetId)) {
            throw new NotFoundException("Relation " + relationId + " does not belong to asset " + assetId);
        }
        return relation;
    }

    private AssetRelation getLegacyRelationBelongingTo(UUID assetId, UUID relationId) {
        var relation = relationRepository
                .findByIdWithEntities(relationId)
                .orElseThrow(() -> new NotFoundException("Relation not found: " + relationId));
        if (!relation.getSource().getId().equals(assetId)
                && !relation.getTarget().getId().equals(assetId)) {
            throw new NotFoundException("Relation " + relationId + " does not belong to asset " + assetId);
        }
        return relation;
    }

    private void applyAssetUpdates(OperationalAsset asset, UpdateAssetCommand command) {
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
    }

    private void validateSameProject(OperationalAsset source, OperationalAsset target) {
        if (!source.getProject().getId().equals(target.getProject().getId())) {
            throw new DomainValidationException("Assets cannot relate across different projects");
        }
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
