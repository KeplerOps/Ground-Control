package com.keplerops.groundcontrol.shared.security;

import com.keplerops.groundcontrol.shared.security.SecurityProperties.ApiCredential;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates callers with {@code Authorization: Bearer <token>} against the configured
 * credential list. On match, sets a {@link UsernamePasswordAuthenticationToken} in the
 * SecurityContext with {@code ROLE_USER} or {@code ROLE_ADMIN}.
 *
 * <p>Constant-time token compare via {@link MessageDigest#isEqual} mirrors the existing pack
 * registry guard convention. Tokens are never logged.
 */
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecurityProperties properties;

    public BearerTokenAuthFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader(AUTHORIZATION_HEADER);
        String token = extractToken(header);
        if (token != null) {
            ApiCredential matched = findMatch(properties.getCredentials(), token);
            if (matched != null) {
                var authority =
                        new SimpleGrantedAuthority("ROLE_" + matched.getRole().name());
                var auth = UsernamePasswordAuthenticationToken.authenticated(
                        matched.getPrincipalName(), null, List.of(authority));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    /*@ pure @*/
    private static String extractToken(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        if (!headerValue.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = headerValue.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /*@ requires credentials != null && presented != null;
    @ pure @*/
    private static ApiCredential findMatch(List<ApiCredential> credentials, String presented) {
        byte[] presentedBytes = presented.getBytes(StandardCharsets.UTF_8);
        ApiCredential match = null;
        for (var cred : credentials) {
            byte[] storedBytes = cred.getToken().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(presentedBytes, storedBytes)) {
                match = cred;
            }
        }
        return match;
    }
}
