package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ObservationService {

    private final ObservationRepository observationRepository;
    private final OperationalAssetRepository assetRepository;

    public ObservationService(ObservationRepository observationRepository, OperationalAssetRepository assetRepository) {
        this.observationRepository = observationRepository;
        this.assetRepository = assetRepository;
    }

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

    public Observation update(UUID assetId, UUID observationId, UpdateObservationCommand command) {
        var observation = getObservationBelongingTo(assetId, observationId);
        if (command.observationValue() != null) {
            observation.setObservationValue(command.observationValue());
        }
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

    @Transactional(readOnly = true)
    public Observation getById(UUID assetId, UUID observationId) {
        return getObservationBelongingTo(assetId, observationId);
    }

    @Transactional(readOnly = true)
    public List<Observation> listByAsset(UUID assetId, ObservationCategory category, String key) {
        verifyAssetExists(assetId);
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
    public List<Observation> listLatest(UUID assetId) {
        verifyAssetExists(assetId);
        return observationRepository.findLatestByAssetId(assetId, Instant.now());
    }

    public void delete(UUID assetId, UUID observationId) {
        observationRepository.delete(getObservationBelongingTo(assetId, observationId));
    }

    private Observation getObservationBelongingTo(UUID assetId, UUID observationId) {
        var observation = observationRepository
                .findByIdWithAsset(observationId)
                .orElseThrow(() -> new NotFoundException("Observation not found: " + observationId));
        if (!observation.getAsset().getId().equals(assetId)) {
            throw new NotFoundException("Observation " + observationId + " does not belong to asset " + assetId);
        }
        return observation;
    }

    private void verifyAssetExists(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found: " + assetId);
        }
    }
}
