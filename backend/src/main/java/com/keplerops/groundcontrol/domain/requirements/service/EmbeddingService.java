package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementEmbedding;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementEmbeddingRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RequirementRepository requirementRepository;
    private final RequirementEmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;

    public EmbeddingService(
            RequirementRepository requirementRepository,
            RequirementEmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider) {
        this.requirementRepository = requirementRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingProvider = embeddingProvider;
    }

    public EmbeddingResult embedRequirement(UUID requirementId) {
        if (!embeddingProvider.isAvailable()) {
            log.debug("embedding_skipped: reason=provider_unavailable requirement_id={}", requirementId);
            return new EmbeddingResult(requirementId, "provider_unavailable", null, null);
        }

        var requirement = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));

        var contentHash = computeHash(requirement);
        var modelId = embeddingProvider.getModelId();

        var existing = embeddingRepository.findByRequirementId(requirementId);
        if (existing.isPresent()) {
            var emb = existing.get();
            if (emb.getContentHash().equals(contentHash) && emb.getModelId().equals(modelId)) {
                log.debug("embedding_up_to_date: requirement_id={}", requirementId);
                return new EmbeddingResult(requirementId, "up_to_date", modelId, contentHash);
            }
            var vector = embeddingProvider.embed(buildText(requirement));
            emb.update(contentHash, vector, modelId);
            embeddingRepository.save(emb);
            log.info("embedding_updated: requirement_id={} model={}", requirementId, modelId);
            return new EmbeddingResult(requirementId, "embedded", modelId, contentHash);
        }

        var vector = embeddingProvider.embed(buildText(requirement));
        var embedding = new RequirementEmbedding(requirement, contentHash, vector, modelId);
        embeddingRepository.save(embedding);
        log.info("embedding_created: requirement_id={} model={}", requirementId, modelId);
        return new EmbeddingResult(requirementId, "embedded", modelId, contentHash);
    }

    public BatchEmbeddingResult embedProject(UUID projectId, boolean force) {
        if (!embeddingProvider.isAvailable()) {
            log.debug("batch_embedding_skipped: reason=provider_unavailable project_id={}", projectId);
            var total = requirementRepository
                    .findByProjectIdAndArchivedAtIsNull(projectId)
                    .size();
            return new BatchEmbeddingResult(total, 0, total, 0, null, List.of("Embedding provider is not configured"));
        }

        var modelId = embeddingProvider.getModelId();
        var requirements = requirementRepository.findByProjectIdAndArchivedAtIsNull(projectId);
        var existingEmbeddings = embeddingRepository.findByRequirementProjectId(projectId);
        var embeddingsByReqId = new java.util.HashMap<UUID, RequirementEmbedding>();
        for (var emb : existingEmbeddings) {
            embeddingsByReqId.put(emb.getRequirement().getId(), emb);
        }

        var toEmbed = new ArrayList<Requirement>();
        int skipped = 0;

        for (var req : requirements) {
            if (force) {
                toEmbed.add(req);
                continue;
            }
            var existing = embeddingsByReqId.get(req.getId());
            if (existing != null
                    && existing.getContentHash().equals(computeHash(req))
                    && existing.getModelId().equals(modelId)) {
                skipped++;
            } else {
                toEmbed.add(req);
            }
        }

        int embedded = 0;
        int failed = 0;
        var errors = new ArrayList<String>();

        // Process in batches
        var batchSize = 50;
        for (int i = 0; i < toEmbed.size(); i += batchSize) {
            var batch = toEmbed.subList(i, Math.min(i + batchSize, toEmbed.size()));
            var texts = batch.stream().map(this::buildText).toList();

            try {
                var vectors = embeddingProvider.embedBatch(texts);
                for (int j = 0; j < batch.size(); j++) {
                    var req = batch.get(j);
                    var vector = vectors.get(j);
                    var contentHash = computeHash(req);
                    var existing = embeddingsByReqId.get(req.getId());

                    if (existing != null) {
                        existing.update(contentHash, vector, modelId);
                        embeddingRepository.save(existing);
                    } else {
                        var embedding = new RequirementEmbedding(req, contentHash, vector, modelId);
                        embeddingRepository.save(embedding);
                    }
                    embedded++;
                }
            } catch (Exception e) {
                log.error("batch_embedding_failed: batch_start={} batch_size={}", i, batch.size(), e);
                failed += batch.size();
                errors.add("Batch " + (i / batchSize) + " failed: " + e.getMessage());
            }
        }

        log.info(
                "batch_embedding_completed: project_id={} total={} embedded={} skipped={} failed={}",
                projectId,
                requirements.size(),
                embedded,
                skipped,
                failed);

        return new BatchEmbeddingResult(requirements.size(), embedded, skipped, failed, modelId, errors);
    }

    @Transactional(readOnly = true)
    public EmbeddingStatus getEmbeddingStatus(UUID requirementId) {
        var requirement = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));

        var currentModelId = embeddingProvider.isAvailable() ? embeddingProvider.getModelId() : null;
        var existing = embeddingRepository.findByRequirementId(requirementId);

        if (existing.isEmpty()) {
            return new EmbeddingStatus(requirementId, false, false, false, currentModelId, null, null);
        }

        var emb = existing.get();
        var contentHash = computeHash(requirement);
        var isStale = !emb.getContentHash().equals(contentHash);
        var modelMismatch = currentModelId != null && !emb.getModelId().equals(currentModelId);

        return new EmbeddingStatus(
                requirementId, true, isStale, modelMismatch, currentModelId, emb.getModelId(), emb.getCreatedAt());
    }

    public void deleteEmbedding(UUID requirementId) {
        embeddingRepository.deleteByRequirementId(requirementId);
        log.info("embedding_deleted: requirement_id={}", requirementId);
    }

    private String buildText(Requirement requirement) {
        return requirement.getTitle() + "\n" + requirement.getStatement() + "\n"
                + (requirement.getRationale() != null ? requirement.getRationale() : "");
    }

    private String computeHash(Requirement requirement) {
        return RequirementEmbedding.computeContentHash(
                requirement.getTitle(), requirement.getStatement(), requirement.getRationale());
    }
}
