package com.keplerops.groundcontrol.domain.requirements.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "requirement_embedding")
public class RequirementEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false, unique = true)
    private Requirement requirement;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] embedding;

    @Column(nullable = false)
    private int dimensions;

    @Column(nullable = false, length = 100)
    private String modelId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RequirementEmbedding() {
        // JPA
    }

    public RequirementEmbedding(Requirement requirement, String contentHash, float[] embeddingVector, String modelId) {
        this.requirement = requirement;
        this.contentHash = contentHash;
        this.embedding = toBytes(embeddingVector);
        this.dimensions = embeddingVector.length;
        this.modelId = modelId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void update(String contentHash, float[] embeddingVector, String modelId) {
        this.contentHash = contentHash;
        this.embedding = toBytes(embeddingVector);
        this.dimensions = embeddingVector.length;
        this.modelId = modelId;
        this.createdAt = Instant.now();
    }

    // --- Conversion utilities ---

    public static byte[] toBytes(float[] floats) {
        var buffer = ByteBuffer.allocate(floats.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    public static float[] toFloats(byte[] data) {
        var buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        var floats = new float[data.length / Float.BYTES];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    public static String computeContentHash(String title, String statement, String rationale) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var text = title + "\n" + statement + "\n" + (rationale != null ? rationale : "");
            digest.update(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // --- Accessors ---

    public UUID getId() {
        return id;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public String getContentHash() {
        return contentHash;
    }

    public float[] getEmbeddingVector() {
        return toFloats(embedding);
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getModelId() {
        return modelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
