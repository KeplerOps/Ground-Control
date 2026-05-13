package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ObservationService {

    private final ObservationRepository observationRepository;
    private final OperationalAssetRepository assetRepository;
    private final FindingLinkRepository findingLinkRepository;

    public ObservationService(
            ObservationRepository observationRepository,
            OperationalAssetRepository assetRepository,
            FindingLinkRepository findingLinkRepository) {
        this.observationRepository = observationRepository;
        this.assetRepository = assetRepository;
        this.findingLinkRepository = findingLinkRepository;
    }

    public Observation create(UUID projectId, UUID assetId, CreateObservationCommand command) {
        var asset = getAssetOrThrow(projectId, assetId);
        if (observationRepository.existsByAssetIdAndCategoryAndObservationKeyAndObservedAt(
                assetId, command.category(), command.observationKey(), command.observedAt())) {
            throw new ConflictException("Observation already exists: " + command.category() + ":"
                    + command.observationKey() + " at " + command.observedAt());
        }
        if (command.expiresAt() != null && command.expiresAt().isBefore(command.observedAt())) {
            throw new DomainValidationException("expiresAt must be after observedAt");
        }
        var observation = new Observation(
                asset,
                command.category(),
                command.observationKey(),
                command.observationValue(),
                command.source(),
                command.observedAt());
        if (command.expiresAt() != null) {
            observation.setExpiresAt(command.expiresAt());
        }
        if (command.confidence() != null) {
            observation.setConfidence(command.confidence());
        }
        if (command.evidenceRef() != null) {
            observation.setEvidenceRef(command.evidenceRef());
        }
        return observationRepository.save(observation);
    }

    @Deprecated(forRemoval = false)
    public Observation create(UUID assetId, CreateObservationCommand command) {
        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetId));
        if (observationRepository.existsByAssetIdAndCategoryAndObservationKeyAndObservedAt(
                assetId, command.category(), command.observationKey(), command.observedAt())) {
            throw new ConflictException("Observation already exists: " + command.category() + ":"
                    + command.observationKey() + " at " + command.observedAt());
        }
        if (command.expiresAt() != null && command.expiresAt().isBefore(command.observedAt())) {
            throw new DomainValidationException("expiresAt must be after observedAt");
        }
        var observation = new Observation(
                asset,
                command.category(),
                command.observationKey(),
                command.observationValue(),
                command.source(),
                command.observedAt());
        if (command.expiresAt() != null) {
            observation.setExpiresAt(command.expiresAt());
        }
        if (command.confidence() != null) {
            observation.setConfidence(command.confidence());
        }
        if (command.evidenceRef() != null) {
            observation.setEvidenceRef(command.evidenceRef());
        }
        return observationRepository.save(observation);
    }

    public Observation update(UUID projectId, UUID assetId, UUID observationId, UpdateObservationCommand command) {
        var observation = getObservationBelongingTo(projectId, assetId, observationId);
        if (command.observationValue() != null) {
            observation.setObservationValue(command.observationValue());
        }
        if (command.expiresAt() != null) {
            if (command.expiresAt().isBefore(observation.getObservedAt())) {
                throw new DomainValidationException("expiresAt must be after observedAt");
            }
            observation.setExpiresAt(command.expiresAt());
        }
        if (command.confidence() != null) {
            observation.setConfidence(command.confidence());
        }
        if (command.evidenceRef() != null) {
            observation.setEvidenceRef(command.evidenceRef());
        }
        return observationRepository.save(observation);
    }

    @Deprecated(forRemoval = false)
    public Observation update(UUID assetId, UUID observationId, UpdateObservationCommand command) {
        var observation = getLegacyObservationBelongingTo(assetId, observationId);
        if (command.observationValue() != null) {
            observation.setObservationValue(command.observationValue());
        }
        if (command.expiresAt() != null) {
            if (command.expiresAt().isBefore(observation.getObservedAt())) {
                throw new DomainValidationException("expiresAt must be after observedAt");
            }
            observation.setExpiresAt(command.expiresAt());
        }
        if (command.confidence() != null) {
            observation.setConfidence(command.confidence());
        }
        if (command.evidenceRef() != null) {
            observation.setEvidenceRef(command.evidenceRef());
        }
        return observationRepository.save(observation);
    }

    @Transactional(readOnly = true)
    public Observation getById(UUID projectId, UUID assetId, UUID observationId) {
        return getObservationBelongingTo(projectId, assetId, observationId);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public Observation getById(UUID assetId, UUID observationId) {
        return getLegacyObservationBelongingTo(assetId, observationId);
    }

    @Transactional(readOnly = true)
    public List<Observation> listByAsset(UUID projectId, UUID assetId, ObservationCategory category, String key) {
        getAssetOrThrow(projectId, assetId);
        if (category != null && key != null) {
            return observationRepository.findByAssetIdAndCategoryAndKey(assetId, category, key);
        }
        if (category != null) {
            return observationRepository.findByAssetIdAndCategory(assetId, category);
        }
        if (key != null) {
            return observationRepository.findByAssetIdAndKey(assetId, key);
        }
        return observationRepository.findByAssetId(assetId);
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<Observation> listByAsset(UUID assetId, ObservationCategory category, String key) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found: " + assetId);
        }
        if (category != null && key != null) {
            return observationRepository.findByAssetIdAndCategoryAndKey(assetId, category, key);
        }
        if (category != null) {
            return observationRepository.findByAssetIdAndCategory(assetId, category);
        }
        if (key != null) {
            return observationRepository.findByAssetIdAndKey(assetId, key);
        }
        return observationRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public List<Observation> listLatest(UUID projectId, UUID assetId) {
        getAssetOrThrow(projectId, assetId);
        return observationRepository.findLatestByAssetId(assetId, Instant.now());
    }

    @Deprecated(forRemoval = false)
    @Transactional(readOnly = true)
    public List<Observation> listLatest(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found: " + assetId);
        }
        return observationRepository.findLatestByAssetId(assetId, Instant.now());
    }

    public void delete(UUID projectId, UUID assetId, UUID observationId) {
        var observation = getObservationBelongingTo(projectId, assetId, observationId);
        rejectIfInboundFindingLinksReferenceObservation(projectId, observationId);
        observationRepository.delete(observation);
    }

    @Deprecated(forRemoval = false)
    public void delete(UUID assetId, UUID observationId) {
        var observation = getLegacyObservationBelongingTo(assetId, observationId);
        rejectIfInboundFindingLinksReferenceObservation(
                observation.getAsset().getProject().getId(), observationId);
        observationRepository.delete(observation);
    }

    private void rejectIfInboundFindingLinksReferenceObservation(UUID projectId, UUID observationId) {
        // FindingLink.targetEntityId is not an FK, so a delete here would leave
        // dangling rows that FindingLinkController.list and the graph projection
        // would happily surface (ADR-038 / cycle-3 codex review on issue #279).
        var inboundFindingUids = findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                FindingLinkTargetType.OBSERVATION, observationId, projectId);
        if (!inboundFindingUids.isEmpty()) {
            Map<String, Serializable> detail = new LinkedHashMap<>();
            detail.put("observationId", observationId.toString());
            detail.put("findingCount", inboundFindingUids.size());
            detail.put("findingUids", new ArrayList<>(inboundFindingUids));
            throw new ConflictException(
                    "Observation " + observationId
                            + " cannot be deleted while inbound FindingLink references exist. Remove the"
                            + " FindingLink references first, then retry.",
                    "observation_referenced",
                    detail);
        }
    }

    private Observation getObservationBelongingTo(UUID projectId, UUID assetId, UUID observationId) {
        var observation = observationRepository
                .findByIdWithAssetAndProjectId(observationId, projectId)
                .orElseThrow(() -> new NotFoundException("Observation not found: " + observationId));
        if (!observation.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Observation " + observationId + " does not belong to asset " + assetId);
        }
        return observation;
    }

    private Observation getLegacyObservationBelongingTo(UUID assetId, UUID observationId) {
        var observation = observationRepository
                .findByIdWithAsset(observationId)
                .orElseThrow(() -> new NotFoundException("Observation not found: " + observationId));
        if (!observation.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Observation " + observationId + " does not belong to asset " + assetId);
        }
        return observation;
    }

    private com.keplerops.groundcontrol.domain.assets.model.OperationalAsset getAssetOrThrow(
            UUID projectId, UUID assetId) {
        return assetRepository
                .findByIdAndProjectId(assetId, projectId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetId));
    }
}
