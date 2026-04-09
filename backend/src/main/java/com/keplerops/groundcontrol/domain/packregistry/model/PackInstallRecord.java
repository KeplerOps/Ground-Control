package com.keplerops.groundcontrol.domain.packregistry.model;

import com.keplerops.groundcontrol.domain.BaseEntity;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Audited
@Table(name = "pack_install_record")
public class PackInstallRecord extends BaseEntity {

    @NotAudited
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "pack_id", nullable = false, length = 200)
    private String packId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pack_type", nullable = false, length = 30)
    private PackType packType;

    @Column(name = "requested_version", length = 50)
    private String requestedVersion;

    @Column(name = "resolved_version", length = 50)
    private String resolvedVersion;

    @Column(name = "resolved_source", length = 2000)
    private String resolvedSource;

    @Column(name = "resolved_checksum", length = 128)
    private String resolvedChecksum;

    @Column(name = "signature_verified")
    private Boolean signatureVerified;

    @Column(name = "trust_policy_id", length = 200)
    private String trustPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_outcome", nullable = false, length = 20)
    private TrustOutcome trustOutcome;

    @Column(name = "trust_reason", columnDefinition = "TEXT")
    private String trustReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "install_outcome", nullable = false, length = 20)
    private InstallOutcome installOutcome;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "installed_entity_id")
    private UUID installedEntityId;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    protected PackInstallRecord() {}

    public PackInstallRecord(
            Project project,
            String packId,
            PackType packType,
            TrustOutcome trustOutcome,
            InstallOutcome installOutcome) {
        this.project = project;
        this.packId = packId;
        this.packType = packType;
        this.trustOutcome = trustOutcome;
        this.installOutcome = installOutcome;
        this.performedAt = Instant.now();
    }

    public Project getProject() {
        return project;
    }

    public String getPackId() {
        return packId;
    }

    public PackType getPackType() {
        return packType;
    }

    public String getRequestedVersion() {
        return requestedVersion;
    }

    public void setRequestedVersion(String requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    public void setResolvedVersion(String resolvedVersion) {
        this.resolvedVersion = resolvedVersion;
    }

    public String getResolvedSource() {
        return resolvedSource;
    }

    public void setResolvedSource(String resolvedSource) {
        this.resolvedSource = resolvedSource;
    }

    public String getResolvedChecksum() {
        return resolvedChecksum;
    }

    public void setResolvedChecksum(String resolvedChecksum) {
        this.resolvedChecksum = resolvedChecksum;
    }

    public Boolean getSignatureVerified() {
        return signatureVerified;
    }

    public void setSignatureVerified(Boolean signatureVerified) {
        this.signatureVerified = signatureVerified;
    }

    public String getTrustPolicyId() {
        return trustPolicyId;
    }

    public void setTrustPolicyId(String trustPolicyId) {
        this.trustPolicyId = trustPolicyId;
    }

    public TrustOutcome getTrustOutcome() {
        return trustOutcome;
    }

    public String getTrustReason() {
        return trustReason;
    }

    public void setTrustReason(String trustReason) {
        this.trustReason = trustReason;
    }

    public InstallOutcome getInstallOutcome() {
        return installOutcome;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public UUID getInstalledEntityId() {
        return installedEntityId;
    }

    public void setInstalledEntityId(UUID installedEntityId) {
        this.installedEntityId = installedEntityId;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
