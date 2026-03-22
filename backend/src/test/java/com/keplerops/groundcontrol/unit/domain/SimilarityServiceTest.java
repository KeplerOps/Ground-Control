package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementEmbedding;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementEmbeddingRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.SimilarityService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimilarityServiceTest {

    @Mock
    private RequirementEmbeddingRepository embeddingRepository;

    @Mock
    private RequirementRepository requirementRepository;

    private SimilarityService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new SimilarityService(embeddingRepository, requirementRepository);
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        return req;
    }

    private static RequirementEmbedding makeEmbedding(Requirement req, float[] vector) {
        return new RequirementEmbedding(req, "hash-" + req.getUid(), vector, "test-model");
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class CosineSimilarity {

        @Test
        void identicalVectors_returnsOne() {
            var a = new float[] {1.0f, 2.0f, 3.0f};
            var b = new float[] {1.0f, 2.0f, 3.0f};
            assertThat(SimilarityService.cosineSimilarity(a, b)).isCloseTo(1.0, within(1e-6));
        }

        @Test
        void orthogonalVectors_returnsZero() {
            var a = new float[] {1.0f, 0.0f, 0.0f};
            var b = new float[] {0.0f, 1.0f, 0.0f};
            assertThat(SimilarityService.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-6));
        }

        @Test
        void oppositeVectors_returnsNegativeOne() {
            var a = new float[] {1.0f, 2.0f, 3.0f};
            var b = new float[] {-1.0f, -2.0f, -3.0f};
            assertThat(SimilarityService.cosineSimilarity(a, b)).isCloseTo(-1.0, within(1e-6));
        }

        @Test
        void zeroVector_returnsZero() {
            var a = new float[] {0.0f, 0.0f, 0.0f};
            var b = new float[] {1.0f, 2.0f, 3.0f};
            assertThat(SimilarityService.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-6));
        }

        @Test
        void similarVectors_returnsHighScore() {
            var a = new float[] {1.0f, 0.0f, 0.0f};
            var b = new float[] {0.9f, 0.1f, 0.0f};
            double score = SimilarityService.cosineSimilarity(a, b);
            assertThat(score).isGreaterThan(0.9);
            assertThat(score).isLessThan(1.0);
        }
    }

    @Nested
    class FindSimilarRequirements {

        @Test
        void noEmbeddings_returnsEmptyResult() {
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of());
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of());

            var result = service.findSimilarRequirements(PROJECT_ID, 0.85);

            assertThat(result.pairs()).isEmpty();
            assertThat(result.pairsAnalyzed()).isZero();
            assertThat(result.embeddedCount()).isZero();
        }

        @Test
        void oneEmbedding_returnsEmptyResult() {
            var req = makeRequirement("REQ-001", UUID.randomUUID());
            var emb = makeEmbedding(req, new float[] {1.0f, 0.0f, 0.0f});

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(emb));

            var result = service.findSimilarRequirements(PROJECT_ID, 0.85);

            assertThat(result.pairs()).isEmpty();
            assertThat(result.embeddedCount()).isEqualTo(1);
        }

        @Test
        void similarPairAboveThreshold_returned() {
            var req1 = makeRequirement("REQ-001", UUID.randomUUID());
            var req2 = makeRequirement("REQ-002", UUID.randomUUID());
            var emb1 = makeEmbedding(req1, new float[] {1.0f, 0.0f, 0.0f});
            var emb2 = makeEmbedding(req2, new float[] {0.95f, 0.05f, 0.0f});

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(emb1, emb2));

            var result = service.findSimilarRequirements(PROJECT_ID, 0.9);

            assertThat(result.pairs()).hasSize(1);
            assertThat(result.pairs().getFirst().uid1()).isEqualTo("REQ-001");
            assertThat(result.pairs().getFirst().uid2()).isEqualTo("REQ-002");
            assertThat(result.pairs().getFirst().score()).isGreaterThan(0.9);
            assertThat(result.pairsAnalyzed()).isEqualTo(1);
        }

        @Test
        void pairBelowThreshold_filtered() {
            var req1 = makeRequirement("REQ-001", UUID.randomUUID());
            var req2 = makeRequirement("REQ-002", UUID.randomUUID());
            var emb1 = makeEmbedding(req1, new float[] {1.0f, 0.0f, 0.0f});
            var emb2 = makeEmbedding(req2, new float[] {0.0f, 1.0f, 0.0f});

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(emb1, emb2));

            var result = service.findSimilarRequirements(PROJECT_ID, 0.85);

            assertThat(result.pairs()).isEmpty();
            assertThat(result.pairsAnalyzed()).isEqualTo(1);
        }

        @Test
        void resultsSortedByScoreDescending() {
            var req1 = makeRequirement("REQ-001", UUID.randomUUID());
            var req2 = makeRequirement("REQ-002", UUID.randomUUID());
            var req3 = makeRequirement("REQ-003", UUID.randomUUID());
            var emb1 = makeEmbedding(req1, new float[] {1.0f, 0.0f, 0.0f});
            var emb2 = makeEmbedding(req2, new float[] {0.9f, 0.1f, 0.0f});
            var emb3 = makeEmbedding(req3, new float[] {0.99f, 0.01f, 0.0f});

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2, req3));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(emb1, emb2, emb3));

            var result = service.findSimilarRequirements(PROJECT_ID, 0.8);

            assertThat(result.pairs()).hasSizeGreaterThanOrEqualTo(2);
            for (int i = 0; i < result.pairs().size() - 1; i++) {
                assertThat(result.pairs().get(i).score())
                        .isGreaterThanOrEqualTo(result.pairs().get(i + 1).score());
            }
        }

        @Test
        void dimensionMismatch_skipped() {
            var req1 = makeRequirement("REQ-001", UUID.randomUUID());
            var req2 = makeRequirement("REQ-002", UUID.randomUUID());
            var emb1 = makeEmbedding(req1, new float[] {1.0f, 0.0f});
            var emb2 = makeEmbedding(req2, new float[] {1.0f, 0.0f, 0.0f});

            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(emb1, emb2));

            var result = service.findSimilarRequirements(PROJECT_ID, 0.5);

            assertThat(result.pairs()).isEmpty();
        }
    }
}
