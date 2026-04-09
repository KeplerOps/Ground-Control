package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistrySecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class PackRegistryAccessGuard {

    private final PackRegistrySecurityProperties securityProperties;

    public PackRegistryAccessGuard(PackRegistrySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /*@ requires request != null;
    @ ensures \result != null; @*/
    public String requireAdminActor(HttpServletRequest request) {
        if (securityProperties.getAdminCredentials().isEmpty()) {
            throw new AuthenticationException("Pack registry admin authentication is not configured");
        }

        var headerValue = request.getHeader(securityProperties.getAuthenticationHeader());
        if (headerValue == null || headerValue.isBlank()) {
            throw new AuthenticationException("Pack registry admin credentials are required");
        }

        var expectedScheme = securityProperties.getTokenScheme();
        var prefix = expectedScheme + " ";
        if (!headerValue.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new AuthenticationException(
                    "Pack registry admin credentials must use the " + expectedScheme + " scheme");
        }

        var token = headerValue.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new AuthenticationException("Pack registry admin token is missing");
        }

        for (var credential : securityProperties.getAdminCredentials()) {
            if (!hasText(credential.getPrincipalName()) || !hasText(credential.getToken())) {
                continue;
            }
            if (MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    credential.getToken().getBytes(StandardCharsets.UTF_8))) {
                return credential.getPrincipalName().trim();
            }
        }

        throw new AuthenticationException("Invalid pack registry admin token");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
