package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.TestUtil;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementEmbedding;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementEmbeddingRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingProvider;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementEmbeddingRepository embeddingRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    private EmbeddingService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();
    private static final String MODEL_ID = "text-embedding-3-small";
    private static final float[] TEST_VECTOR = {0.1f, 0.2f, 0.3f};

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new EmbeddingService(requirementRepository, embeddingRepository, embeddingProvider);
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        TestUtil.setField(obj, fieldName, value);
    }

    @Nested
    class EmbedRequirement {

        @Test
        void providerUnavailable_returnsProviderUnavailableStatus() {
            when(embeddingProvider.isAvailable()).thenReturn(false);

            var result = service.embedRequirement(UUID.randomUUID());

            assertThat(result.status()).isEqualTo("provider_unavailable");
            verify(requirementRepository, never()).findById(any());
        }

        @Test
        void requirementNotFound_throwsNotFoundException() {
            var reqId = UUID.randomUUID();
            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(requirementRepository.findById(reqId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.embedRequirement(reqId)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void embeddingUpToDate_returnsUpToDateStatus() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var contentHash =
                    RequirementEmbedding.computeContentHash(req.getTitle(), req.getStatement(), req.getRationale());
            var existing = new RequirementEmbedding(req, contentHash, TEST_VECTOR, MODEL_ID);

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var result = service.embedRequirement(reqId);

            assertThat(result.status()).isEqualTo("up_to_date");
            verify(embeddingProvider, never()).embed(any());
        }

        @Test
        void embeddingStale_recomputesEmbedding() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var existing = new RequirementEmbedding(req, "old_hash", TEST_VECTOR, MODEL_ID);

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingProvider.embed(any())).thenReturn(new float[] {0.4f, 0.5f, 0.6f});
            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var result = service.embedRequirement(reqId);

            assertThat(result.status()).isEqualTo("embedded");
            verify(embeddingProvider).embed(any());
            verify(embeddingRepository).save(existing);
        }

        @Test
        void modelMismatch_recomputesEmbedding() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var contentHash =
                    RequirementEmbedding.computeContentHash(req.getTitle(), req.getStatement(), req.getRationale());
            var existing = new RequirementEmbedding(req, contentHash, TEST_VECTOR, "old-model");

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingProvider.embed(any())).thenReturn(new float[] {0.4f, 0.5f, 0.6f});
            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var result = service.embedRequirement(reqId);

            assertThat(result.status()).isEqualTo("embedded");
            assertThat(result.modelId()).isEqualTo(MODEL_ID);
        }

        @Test
        void noExistingEmbedding_createsNew() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingProvider.embed(any())).thenReturn(TEST_VECTOR);
            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.empty());

            var result = service.embedRequirement(reqId);

            assertThat(result.status()).isEqualTo("embedded");
            assertThat(result.modelId()).isEqualTo(MODEL_ID);
            verify(embeddingRepository).save(any(RequirementEmbedding.class));
        }
    }

    @Nested
    class EmbedProject {

        @Test
        void providerUnavailable_returnsSkippedResult() {
            when(embeddingProvider.isAvailable()).thenReturn(false);
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(makeRequirement("REQ-001", UUID.randomUUID())));

            var result = service.embedProject(PROJECT_ID, false);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.embedded()).isZero();
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.errors()).hasSize(1);
        }

        @Test
        void batchEmbedding_countsCorrectly() {
            var reqId1 = UUID.randomUUID();
            var reqId2 = UUID.randomUUID();
            var req1 = makeRequirement("REQ-001", reqId1);
            var req2 = makeRequirement("REQ-002", reqId2);
            var hash1 =
                    RequirementEmbedding.computeContentHash(req1.getTitle(), req1.getStatement(), req1.getRationale());
            var existing1 = new RequirementEmbedding(req1, hash1, TEST_VECTOR, MODEL_ID);

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingProvider.embedBatch(anyList())).thenReturn(List.of(TEST_VECTOR));
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req1, req2));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(existing1));

            var result = service.embedProject(PROJECT_ID, false);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.embedded()).isEqualTo(1);
            assertThat(result.failed()).isZero();
        }

        @Test
        void forceFlag_reembeddsAll() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var hash = RequirementEmbedding.computeContentHash(req.getTitle(), req.getStatement(), req.getRationale());
            var existing = new RequirementEmbedding(req, hash, TEST_VECTOR, MODEL_ID);

            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingProvider.embedBatch(anyList())).thenReturn(List.of(TEST_VECTOR));
            when(requirementRepository.findByProjectIdAndArchivedAtIsNull(PROJECT_ID))
                    .thenReturn(List.of(req));
            when(embeddingRepository.findByRequirementProjectId(PROJECT_ID)).thenReturn(List.of(existing));

            var result = service.embedProject(PROJECT_ID, true);

            assertThat(result.skipped()).isZero();
            assertThat(result.embedded()).isEqualTo(1);
        }
    }

    @Nested
    class GetEmbeddingStatus {

        @Test
        void noEmbedding_returnsNotEmbedded() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);

            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.empty());

            var status = service.getEmbeddingStatus(reqId);

            assertThat(status.hasEmbedding()).isFalse();
            assertThat(status.isStale()).isFalse();
            assertThat(status.modelMismatch()).isFalse();
        }

        @Test
        void upToDateEmbedding_returnsCorrectStatus() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var contentHash =
                    RequirementEmbedding.computeContentHash(req.getTitle(), req.getStatement(), req.getRationale());
            var existing = new RequirementEmbedding(req, contentHash, TEST_VECTOR, MODEL_ID);

            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var status = service.getEmbeddingStatus(reqId);

            assertThat(status.hasEmbedding()).isTrue();
            assertThat(status.isStale()).isFalse();
            assertThat(status.modelMismatch()).isFalse();
        }

        @Test
        void staleEmbedding_detectsStaleness() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var existing = new RequirementEmbedding(req, "old_hash", TEST_VECTOR, MODEL_ID);

            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var status = service.getEmbeddingStatus(reqId);

            assertThat(status.hasEmbedding()).isTrue();
            assertThat(status.isStale()).isTrue();
        }

        @Test
        void modelMismatch_detectsMismatch() {
            var reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-001", reqId);
            var contentHash =
                    RequirementEmbedding.computeContentHash(req.getTitle(), req.getStatement(), req.getRationale());
            var existing = new RequirementEmbedding(req, contentHash, TEST_VECTOR, "old-model");

            when(requirementRepository.findById(reqId)).thenReturn(Optional.of(req));
            when(embeddingProvider.isAvailable()).thenReturn(true);
            when(embeddingProvider.getModelId()).thenReturn(MODEL_ID);
            when(embeddingRepository.findByRequirementId(reqId)).thenReturn(Optional.of(existing));

            var status = service.getEmbeddingStatus(reqId);

            assertThat(status.hasEmbedding()).isTrue();
            assertThat(status.modelMismatch()).isTrue();
            assertThat(status.embeddingModelId()).isEqualTo("old-model");
            assertThat(status.currentModelId()).isEqualTo(MODEL_ID);
        }
    }
}
