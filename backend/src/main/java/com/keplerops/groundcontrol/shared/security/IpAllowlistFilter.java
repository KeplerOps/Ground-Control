package com.keplerops.groundcontrol.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.api.ErrorResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests whose source address is outside the configured CIDR allowlist.
 *
 * <p>An empty allowlist is a pass-through — IP filtering is opt-in. Only the connection's
 * {@code remoteAddr} is consulted; X-Forwarded-For is intentionally NOT trusted, since trusting
 * client-supplied headers without a configured proxy would let attackers bypass the gate.
 */
public class IpAllowlistFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;
    private List<IpAddressMatcher> matchers = List.of();

    public IpAllowlistFilter(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void compile() {
        var compiled =
                new ArrayList<IpAddressMatcher>(properties.getIpAllowlist().size());
        for (String cidr : properties.getIpAllowlist()) {
            if (cidr == null || cidr.isBlank()) {
                throw new IllegalStateException(
                        "groundcontrol.security.ipAllowlist contains a blank entry; remove it or supply a CIDR");
            }
            try {
                compiled.add(new IpAddressMatcher(cidr));
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(
                        "groundcontrol.security.ipAllowlist contains an invalid CIDR: " + cidr, ex);
            }
        }
        this.matchers = List.copyOf(compiled);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || matchers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        String remote = request.getRemoteAddr();
        if (remote != null && matches(remote)) {
            chain.doFilter(request, response);
            return;
        }
        writeAccessDenied(response);
    }

    /*@ pure @*/
    private boolean matches(String remoteAddr) {
        for (var matcher : matchers) {
            if (matcher.matches(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    private void writeAccessDenied(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var body = ErrorResponse.of("access_denied", "Source IP is not in the allowlist");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
