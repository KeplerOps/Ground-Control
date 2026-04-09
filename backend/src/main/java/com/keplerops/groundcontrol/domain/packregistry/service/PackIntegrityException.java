package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class PackIntegrityException extends DomainValidationException {

    private final String verifiedChecksum;
    private final boolean checksumVerified;
    private final Boolean signatureVerified;

    public PackIntegrityException(
            String message, String verifiedChecksum, boolean checksumVerified, Boolean signatureVerified) {
        super(
                message,
                "integrity_verification_failed",
                buildDetail(verifiedChecksum, checksumVerified, signatureVerified));
        this.verifiedChecksum = verifiedChecksum;
        this.checksumVerified = checksumVerified;
        this.signatureVerified = signatureVerified;
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

    private static Map<String, Serializable> buildDetail(
            String verifiedChecksum, boolean checksumVerified, Boolean signatureVerified) {
        var detail = new LinkedHashMap<String, Serializable>();
        if (verifiedChecksum != null) {
            detail.put("verifiedChecksum", verifiedChecksum);
        }
        detail.put("checksumVerified", checksumVerified);
        if (signatureVerified != null) {
            detail.put("signatureVerified", signatureVerified);
        }
        return detail;
    }
}
