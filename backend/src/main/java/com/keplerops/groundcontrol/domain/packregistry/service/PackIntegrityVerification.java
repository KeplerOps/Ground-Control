package com.keplerops.groundcontrol.domain.packregistry.service;

public record PackIntegrityVerification(
        String verifiedChecksum, boolean checksumVerified, Boolean signatureVerified, Boolean signerTrusted) {}
