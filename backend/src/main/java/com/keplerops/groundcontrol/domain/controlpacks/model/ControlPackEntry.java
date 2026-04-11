package com.keplerops.groundcontrol.domain.controlpacks.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackEntryStatus;
import com.keplerops.groundcontrol.domain.controls.model.Control;
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
import java.util.List;
import java.util.Map;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(
        name = "control_pack_entry",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"control_pack_id", "entry_uid"}),
            @UniqueConstraint(columnNames = {"control_pack_id", "control_id"})
        })
public class ControlPackEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_pack_id", nullable = false)
    private ControlPack controlPack;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Column(name = "entry_uid", nullable = false, length = 50)
    private String entryUid;

    @Convert(converter = JacksonTextCollectionConverters.StringObjectMapConverter.class)
    @Column(name = "original_definition", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> originalDefinition;

    @Convert(converter = JacksonTextCollectionConverters.MapListConverter.class)
    @Column(name = "expected_evidence", columnDefinition = "TEXT")
    private List<Map<String, Object>> expectedEvidence;

    @Column(name = "implementation_guidance", columnDefinition = "TEXT")
    private String implementationGuidance;

    @Convert(converter = JacksonTextCollectionConverters.MapListConverter.class)
    @Column(name = "framework_mappings", columnDefinition = "TEXT")
    private List<Map<String, Object>> frameworkMappings;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status", nullable = false, length = 20)
    private ControlPackEntryStatus entryStatus = ControlPackEntryStatus.ACTIVE;

    protected ControlPackEntry() {
        // JPA
    }

    public ControlPackEntry(ControlPack controlPack, Control control, String entryUid) {
        this.controlPack = controlPack;
        this.control = control;
        this.entryUid = entryUid;
    }

    public ControlPack getControlPack() {
        return controlPack;
    }

    public Control getControl() {
        return control;
    }

    public String getEntryUid() {
        return entryUid;
    }

    public Map<String, Object> getOriginalDefinition() {
        return originalDefinition;
    }

    public void setOriginalDefinition(Map<String, Object> originalDefinition) {
        this.originalDefinition = originalDefinition;
    }

    public List<Map<String, Object>> getExpectedEvidence() {
        return expectedEvidence;
    }

    public void setExpectedEvidence(List<Map<String, Object>> expectedEvidence) {
        this.expectedEvidence = expectedEvidence;
    }

    public String getImplementationGuidance() {
        return implementationGuidance;
    }

    public void setImplementationGuidance(String implementationGuidance) {
        this.implementationGuidance = implementationGuidance;
    }

    public List<Map<String, Object>> getFrameworkMappings() {
        return frameworkMappings;
    }

    public void setFrameworkMappings(List<Map<String, Object>> frameworkMappings) {
        this.frameworkMappings = frameworkMappings;
    }

    public ControlPackEntryStatus getEntryStatus() {
        return entryStatus;
    }

    public void setEntryStatus(ControlPackEntryStatus entryStatus) {
        this.entryStatus = entryStatus;
    }
}
