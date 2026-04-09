package com.keplerops.groundcontrol.domain.packregistry.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ground-control.pack-registry.security")
public class PackRegistrySecurityProperties {

    private String authenticationHeader = "Authorization";
    private String tokenScheme = "Bearer";
    private List<AdminCredential> adminCredentials = new ArrayList<>();
    private List<TrustedSigner> trustedSigners = new ArrayList<>();

    public String getAuthenticationHeader() {
        return authenticationHeader;
    }

    public void setAuthenticationHeader(String authenticationHeader) {
        this.authenticationHeader = authenticationHeader;
    }

    public String getTokenScheme() {
        return tokenScheme;
    }

    public void setTokenScheme(String tokenScheme) {
        this.tokenScheme = tokenScheme;
    }

    public List<AdminCredential> getAdminCredentials() {
        return adminCredentials;
    }

    public void setAdminCredentials(List<AdminCredential> adminCredentials) {
        this.adminCredentials = adminCredentials != null ? adminCredentials : new ArrayList<>();
    }

    public List<TrustedSigner> getTrustedSigners() {
        return trustedSigners;
    }

    public void setTrustedSigners(List<TrustedSigner> trustedSigners) {
        this.trustedSigners = trustedSigners != null ? trustedSigners : new ArrayList<>();
    }

    public static class AdminCredential {

        private String principalName;
        private String token;

        public String getPrincipalName() {
            return principalName;
        }

        public void setPrincipalName(String principalName) {
            this.principalName = principalName;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
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
