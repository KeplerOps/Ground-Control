package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PackIntegrityException extends DomainValidationException {

    private final String verifiedChecksum;
    private final boolean checksumVerified;
    private final Boolean signatureVerified;
    private final Boolean signerTrusted;

    public PackIntegrityException(
            String message, String verifiedChecksum, boolean checksumVerified, Boolean signatureVerified) {
        this(message, verifiedChecksum, checksumVerified, signatureVerified, null);
    }

    public PackIntegrityException(
            String message,
            String verifiedChecksum,
            boolean checksumVerified,
            Boolean signatureVerified,
            Boolean signerTrusted) {
        super(
                message,
                "integrity_verification_failed",
                buildDetail(verifiedChecksum, checksumVerified, signatureVerified, signerTrusted));
        this.verifiedChecksum = verifiedChecksum;
        this.checksumVerified = checksumVerified;
        this.signatureVerified = signatureVerified;
        this.signerTrusted = signerTrusted;
    }

    public String getVerifiedChecksum() {
        return verifiedChecksum;
    }

    public boolean isChecksumVerified() {
        return checksumVerified;
    }

    public Boolean getSignatureVerified() {
        return signatureVerified;
    }

    public Boolean getSignerTrusted() {
        return signerTrusted;
    }

    private static Map<String, Serializable> buildDetail(
            String verifiedChecksum, boolean checksumVerified, Boolean signatureVerified, Boolean signerTrusted) {
        var detail = new LinkedHashMap<String, Serializable>();
        if (verifiedChecksum != null) {
            detail.put("verifiedChecksum", verifiedChecksum);
        }
        detail.put("checksumVerified", checksumVerified);
        if (signatureVerified != null) {
            detail.put("signatureVerified", signatureVerified);
        }
        if (signerTrusted != null) {
            detail.put("signerTrusted", signerTrusted);
        }
        return detail;
    }
}
