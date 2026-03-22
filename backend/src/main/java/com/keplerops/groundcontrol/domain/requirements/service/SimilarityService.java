package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.repository.RequirementEmbeddingRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SimilarityService {

    private static final Logger log = LoggerFactory.getLogger(SimilarityService.class);

    private final RequirementEmbeddingRepository embeddingRepository;
    private final RequirementRepository requirementRepository;

    public SimilarityService(
            RequirementEmbeddingRepository embeddingRepository, RequirementRepository requirementRepository) {
        this.embeddingRepository = embeddingRepository;
        this.requirementRepository = requirementRepository;
    }

    public SimilarityResult findSimilarRequirements(UUID projectId, double threshold) {
        var totalRequirements = requirementRepository
                .findByProjectIdAndArchivedAtIsNull(projectId)
                .size();
        var embeddings = embeddingRepository.findByRequirementProjectId(projectId);

        if (embeddings.size() < 2) {
            log.debug("similarity_skipped: reason=insufficient_embeddings count={}", embeddings.size());
            return new SimilarityResult(totalRequirements, embeddings.size(), 0, threshold, List.of());
        }

        var pairs = new ArrayList<SimilarityPair>();
        int pairsAnalyzed = 0;

        for (int i = 0; i < embeddings.size(); i++) {
            for (int j = i + 1; j < embeddings.size(); j++) {
                pairsAnalyzed++;
                var a = embeddings.get(i);
                var b = embeddings.get(j);

                var vecA = a.getEmbeddingVector();
                var vecB = b.getEmbeddingVector();

                if (vecA.length != vecB.length || vecA.length == 0) {
                    continue;
                }

                double score = cosineSimilarity(vecA, vecB);
                if (score >= threshold) {
                    pairs.add(new SimilarityPair(
                            a.getRequirement().getUid(),
                            a.getRequirement().getTitle(),
                            b.getRequirement().getUid(),
                            b.getRequirement().getTitle(),
                            score));
                }
            }
        }

        pairs.sort(Comparator.comparingDouble(SimilarityPair::score).reversed());

        log.info(
                "similarity_analysis_completed: project_id={} embeddings={} pairs_analyzed={} matches={}",
                projectId,
                embeddings.size(),
                pairsAnalyzed,
                pairs.size());

        return new SimilarityResult(totalRequirements, embeddings.size(), pairsAnalyzed, threshold, pairs);
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dotProduct / denominator;
    }
}
