package com.keplerops.groundcontrol.domain.controlpacks.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(
        name = "control_pack_override",
        uniqueConstraints = @UniqueConstraint(columnNames = {"control_pack_entry_id", "field_name"}))
public class ControlPackOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_pack_entry_id", nullable = false)
    private ControlPackEntry controlPackEntry;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "override_value", columnDefinition = "TEXT")
    private String overrideValue;

    @Column(length = 500)
    private String reason;

    protected ControlPackOverride() {
        // JPA
    }

    public ControlPackOverride(
            ControlPackEntry controlPackEntry, String fieldName, String overrideValue, String reason) {
        this.controlPackEntry = controlPackEntry;
        this.fieldName = fieldName;
        this.overrideValue = overrideValue;
        this.reason = reason;
    }

    public ControlPackEntry getControlPackEntry() {
        return controlPackEntry;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(String overrideValue) {
        this.overrideValue = overrideValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
