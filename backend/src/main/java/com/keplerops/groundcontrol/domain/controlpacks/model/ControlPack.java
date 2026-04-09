package com.keplerops.groundcontrol.domain.controlpacks.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackLifecycleState;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.shared.persistence.JacksonTextCollectionConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "control_pack", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "pack_id"}))
public class ControlPack extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "pack_id", nullable = false, length = 200)
    private String packId;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(length = 200)
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(length = 128)
    private String checksum;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> compatibility;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(name = "pack_metadata", columnDefinition = "TEXT")
    private Map<String, Object> packMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 20)
    private ControlPackLifecycleState lifecycleState = ControlPackLifecycleState.INSTALLED;

    @Column(name = "installed_at", nullable = false)
    private Instant installedAt;

    protected ControlPack() {
        // JPA
    }

    public ControlPack(Project project, String packId, String version) {
        this.project = project;
        this.packId = packId;
        this.version = version;
        this.installedAt = Instant.now();
    }

    public void transitionLifecycleState(ControlPackLifecycleState newState) {
        if (newState == null || !lifecycleState.canTransitionTo(newState)) {
            throw new DomainValidationException(
                    "Cannot transition control pack lifecycle from " + lifecycleState + " to " + newState,
                    "invalid_lifecycle_transition",
                    Map.of("current", lifecycleState.name(), "requested", String.valueOf(newState)));
        }
        this.lifecycleState = newState;
    }

    public Project getProject() {
        return project;
    }

    public String getPackId() {
        return packId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Map<String, Object> getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(Map<String, Object> compatibility) {
        this.compatibility = compatibility;
    }

    public Map<String, Object> getPackMetadata() {
        return packMetadata;
    }

    public void setPackMetadata(Map<String, Object> packMetadata) {
        this.packMetadata = packMetadata;
    }

    public ControlPackLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }
}
