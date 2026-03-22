package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementEmbedding;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RequirementEmbeddingTest {

    @Nested
    class ContentHash {

        @Test
        void deterministic_samInputProducesSameHash() {
            var hash1 = RequirementEmbedding.computeContentHash("Title", "Statement", "Rationale");
            var hash2 = RequirementEmbedding.computeContentHash("Title", "Statement", "Rationale");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        void changesWhenTitleChanges() {
            var hash1 = RequirementEmbedding.computeContentHash("Title A", "Statement", "Rationale");
            var hash2 = RequirementEmbedding.computeContentHash("Title B", "Statement", "Rationale");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        void changesWhenStatementChanges() {
            var hash1 = RequirementEmbedding.computeContentHash("Title", "Statement A", "Rationale");
            var hash2 = RequirementEmbedding.computeContentHash("Title", "Statement B", "Rationale");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        void changesWhenRationaleChanges() {
            var hash1 = RequirementEmbedding.computeContentHash("Title", "Statement", "Rationale A");
            var hash2 = RequirementEmbedding.computeContentHash("Title", "Statement", "Rationale B");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        void nullRationaleTreatedAsEmpty() {
            var hash1 = RequirementEmbedding.computeContentHash("Title", "Statement", null);
            var hash2 = RequirementEmbedding.computeContentHash("Title", "Statement", "");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        void hashIsSha256HexLength() {
            var hash = RequirementEmbedding.computeContentHash("Title", "Statement", "Rationale");
            assertThat(hash).hasSize(64);
            assertThat(hash).matches("[0-9a-f]{64}");
        }
    }

    @Nested
    class ByteConversion {

        @Test
        void roundTrip_preservesFidelity() {
            var original = new float[] {1.0f, -1.0f, 0.0f, 3.14159f, Float.MAX_VALUE, Float.MIN_VALUE};
            var bytes = RequirementEmbedding.toBytes(original);
            var restored = RequirementEmbedding.toFloats(bytes);
            assertThat(restored).containsExactly(original);
        }

        @Test
        void emptyArray_roundTrips() {
            var original = new float[0];
            var bytes = RequirementEmbedding.toBytes(original);
            var restored = RequirementEmbedding.toFloats(bytes);
            assertThat(restored).isEmpty();
        }

        @Test
        void byteSizeIsCorrect() {
            var floats = new float[] {1.0f, 2.0f, 3.0f};
            var bytes = RequirementEmbedding.toBytes(floats);
            assertThat(bytes).hasSize(3 * Float.BYTES);
        }
    }
}
