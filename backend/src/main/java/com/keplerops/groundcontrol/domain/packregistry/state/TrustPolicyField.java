package com.keplerops.groundcontrol.domain.packregistry.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum TrustPolicyField {
    PUBLISHER("publisher"),
    PACK_ID("packId"),
    PACK_TYPE("packType"),
    VERSION("version"),
    SOURCE_URL("sourceUrl"),
    CHECKSUM("checksum"),
    VERIFIED_CHECKSUM("verifiedChecksum"),
    CHECKSUM_VERIFIED("checksumVerified"),
    SIGNATURE_VERIFIED("signatureVerified"),
    SIGNER_TRUSTED("signerTrusted");

    private final String wireValue;

    TrustPolicyField(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static TrustPolicyField fromWireValue(String wireValue) {
        return Arrays.stream(values())
                .filter(value -> value.wireValue.equals(wireValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported trust policy field: " + wireValue));
    }
}
