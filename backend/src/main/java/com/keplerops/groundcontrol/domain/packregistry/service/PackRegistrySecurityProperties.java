package com.keplerops.groundcontrol.domain.packregistry.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pack registry signing configuration. Admin authentication for pack registry endpoints is now
 * handled by the unified {@code groundcontrol.security} layer (Spring Security with bearer
 * tokens); this properties class only carries the list of trusted signers used by
 * {@link PackIntegrityVerifier} to validate signed pack imports.
 */
@ConfigurationProperties(prefix = "ground-control.pack-registry.security")
public class PackRegistrySecurityProperties {

    private List<TrustedSigner> trustedSigners = new ArrayList<>();

    public List<TrustedSigner> getTrustedSigners() {
        return trustedSigners;
    }

    public void setTrustedSigners(List<TrustedSigner> trustedSigners) {
        this.trustedSigners = trustedSigners != null ? trustedSigners : new ArrayList<>();
    }

    public static class TrustedSigner {

        private String keyId;
        private String publicKey;
        private String publisher;
        private String packId;

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getPackId() {
            return packId;
        }

        public void setPackId(String packId) {
            this.packId = packId;
        }
    }
}
