package com.keplerops.groundcontrol.domain.packregistry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.packregistry.model.PackDependency;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.model.RegisteredControlPackEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PackIntegrityVerifierTest {

    private final PackRegistrySecurityProperties securityProperties = new PackRegistrySecurityProperties();
    private final PackIntegrityVerifier verifier = new PackIntegrityVerifier(securityProperties);

    @Test
    void computesChecksumForUnsignedPack() {
        var verification = verifier.verify(makeEntry());

        assertThat(verification.verifiedChecksum()).startsWith("sha256:");
        assertThat(verification.checksumVerified()).isFalse();
        assertThat(verification.signatureVerified()).isNull();
        assertThat(verification.signerTrusted()).isNull();
    }

    @Test
    void rejectsChecksumMismatch() {
        var entry = makeEntry();
        entry.setChecksum("sha256:0000000000000000000000000000000000000000000000000000000000000000");

        assertThatThrownBy(() -> verifier.verify(entry))
                .isInstanceOf(PackIntegrityException.class)
                .hasMessageContaining("checksum mismatch");
    }

    @Test
    void verifiesDetachedSignature() throws Exception {
        var entry = makeEntry();
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(verifier.canonicalPayloadBytes(entry));
        entry.setSignatureInfo(Map.of(
                "algorithm", "SHA256withRSA",
                "publicKey",
                        Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                "signature", Base64.getEncoder().encodeToString(signer.sign())));

        var verification = verifier.verify(entry);

        assertThat(verification.signatureVerified()).isTrue();
        assertThat(verification.signerTrusted()).isFalse();
    }

    @Test
    void trustsDetachedSignatureOnlyWhenSignerKeyIsConfigured() throws Exception {
        var entry = makeEntry();
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        var signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(verifier.canonicalPayloadBytes(entry));
        var encodedPublicKey =
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        entry.setSignatureInfo(Map.of(
                "algorithm",
                "SHA256withRSA",
                "publicKey",
                encodedPublicKey,
                "signature",
                Base64.getEncoder().encodeToString(signer.sign())));
        var trustedSigner = new PackRegistrySecurityProperties.TrustedSigner();
        trustedSigner.setKeyId("nist-signing-key");
        trustedSigner.setPublisher("NIST");
        trustedSigner.setPublicKey(encodedPublicKey);
        securityProperties.setTrustedSigners(List.of(trustedSigner));

        var verification = verifier.verify(entry);

        assertThat(verification.signatureVerified()).isTrue();
        assertThat(verification.signerTrusted()).isTrue();
    }

    @Test
    void rejectsWeakSignatureAlgorithm() throws Exception {
        var entry = makeEntry();
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        var signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(verifier.canonicalPayloadBytes(entry));
        entry.setSignatureInfo(Map.of(
                "algorithm", "SHA1withRSA",
                "publicKey",
                        Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                "signature", Base64.getEncoder().encodeToString(signer.sign())));

        assertThatThrownBy(() -> verifier.verify(entry))
                .isInstanceOf(PackIntegrityException.class)
                .hasMessageContaining("Unsupported signature algorithm");
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        var entry = makeEntry();
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        entry.setSignatureInfo(Map.of(
                "algorithm", "SHA256withRSA",
                "publicKey",
                        Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                "signature",
                        Base64.getEncoder().encodeToString("tampered-signature".getBytes(StandardCharsets.UTF_8))));

        assertThatThrownBy(() -> verifier.verify(entry))
                .isInstanceOf(PackIntegrityException.class)
                .satisfies(exception -> {
                    var integrityException = (PackIntegrityException) exception;
                    assertThat(integrityException.getSignatureVerified()).isFalse();
                });
    }

    private PackRegistryEntry makeEntry() {
        var project = new Project("ground-control", "Ground Control");
        var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
        entry.setPublisher("NIST");
        entry.setDescription("NIST controls");
        entry.setSourceUrl("https://registry.example.com/nist-800-53");
        entry.setCompatibility(Map.of("minVersion", "0.1.0"));
        entry.setDependencies(List.of(new PackDependency("foundation-pack", "^1.0.0")));
        entry.setControlPackEntries(List.of(new RegisteredControlPackEntry(
                "AC-1",
                "Access Control Policy",
                ControlFunction.PREVENTIVE,
                "Policy definition",
                "Define baseline policy",
                "Security",
                "Global",
                null,
                null,
                "Access Control",
                "NIST",
                null,
                null,
                null)));
        entry.setRegistryMetadata(Map.of("channel", "stable"));
        return entry;
    }
}
