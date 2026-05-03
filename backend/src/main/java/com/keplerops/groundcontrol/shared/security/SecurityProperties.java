package com.keplerops.groundcontrol.shared.security;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Configuration for the API access control layer.
 *
 * <p>Driven from {@code groundcontrol.security.*} — operators rotate credentials, adjust the IP
 * allowlist, or toggle OpenAPI exposure without code changes.
 */
@ConfigurationProperties(prefix = "groundcontrol.security")
public class SecurityProperties {

    private boolean enabled = true;
    private List<ApiCredential> credentials = new ArrayList<>();
    private List<String> ipAllowlist = new ArrayList<>();
    private boolean openapiPublic = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ApiCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<ApiCredential> credentials) {
        this.credentials = credentials != null ? credentials : new ArrayList<>();
    }

    public List<String> getIpAllowlist() {
        return ipAllowlist;
    }

    public void setIpAllowlist(List<String> ipAllowlist) {
        this.ipAllowlist = ipAllowlist != null ? ipAllowlist : new ArrayList<>();
    }

    public boolean isOpenapiPublic() {
        return openapiPublic;
    }

    public void setOpenapiPublic(boolean openapiPublic) {
        this.openapiPublic = openapiPublic;
    }

    /**
     * Validate the configured properties. Called after Spring binds the bean so misconfiguration
     * surfaces at startup rather than on the first request. Public so unit tests can drive it
     * directly without bootstrapping a Spring context.
     */
    @PostConstruct
    public void validate() {
        var seenTokens = new HashSet<String>();
        for (var cred : credentials) {
            cred.validate();
            if (!seenTokens.add(cred.getToken())) {
                throw new IllegalStateException(
                        "groundcontrol.security.credentials contains duplicate tokens; each token must be unique");
            }
        }
        for (var cidr : ipAllowlist) {
            if (cidr == null || cidr.isBlank()) {
                throw new IllegalStateException(
                        "groundcontrol.security.ipAllowlist contains a blank entry; remove it or supply a CIDR");
            }
            try {
                new IpAddressMatcher(cidr);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(
                        "groundcontrol.security.ipAllowlist contains an invalid CIDR: " + cidr, ex);
            }
        }
    }

    public enum Role {
        USER,
        ADMIN
    }

    public static class ApiCredential {

        private String principalName;
        private String token;
        private Role role;

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

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public void validate() {
            if (principalName == null || principalName.isBlank()) {
                throw new IllegalStateException(
                        "groundcontrol.security.credentials[].principalName is required and must be non-blank");
            }
            if (token == null || token.isBlank()) {
                throw new IllegalStateException(
                        "groundcontrol.security.credentials[].token is required and must be non-blank");
            }
            if (role == null) {
                throw new IllegalStateException(
                        "groundcontrol.security.credentials[].role is required (USER or ADMIN)");
            }
        }
    }
}
