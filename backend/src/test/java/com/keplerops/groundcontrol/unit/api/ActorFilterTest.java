package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.shared.web.ActorFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ActorFilterTest {

    private final ActorFilter filter = new ActorFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearsActorAfterFilterChain() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "test-user");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(ActorHolder.get()).isNull();
        assertThat(MDC.get("actor")).isNull();
    }

    @Test
    void defaultsToAnonymousWhenHeaderMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("anonymous");
    }

    @Test
    void defaultsToAnonymousWhenHeaderBlank() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "   ");
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("anonymous");
    }

    @Test
    void setsActorHolderAndMdcFromHeader_whenNoAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "alice");
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("alice");
        assertThat(chain.capturedMdc).isEqualTo("alice");
        assertThat(ActorHolder.get()).isNull();
        assertThat(MDC.get("actor")).isNull();
    }

    @Test
    void prefersAuthenticatedPrincipal_overXActorHeader() throws Exception {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "bob", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "spoofed");
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("bob");
        assertThat(chain.capturedMdc).isEqualTo("bob");
    }

    @Test
    void usesAuthenticatedPrincipal_whenHeaderMissing() throws Exception {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "carol", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("carol");
    }

    @Test
    void unauthenticatedToken_isIgnored() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("dave", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "fallback-user");
        var response = new MockHttpServletResponse();
        var chain = capturingChain();

        filter.doFilter(request, response, chain);

        // Unauthenticated Authentication objects must NOT short-circuit the audit identity. The
        // X-Actor header should win in that case so we never silently treat unauthenticated state
        // as an authenticated principal.
        assertThat(chain.capturedActor).isEqualTo("fallback-user");
    }

    private static CapturingChain capturingChain() {
        return new CapturingChain();
    }

    private static final class CapturingChain extends MockFilterChain {
        String capturedActor;
        String capturedMdc;

        @Override
        public void doFilter(ServletRequest req, ServletResponse res) {
            capturedActor = ActorHolder.get();
            capturedMdc = MDC.get("actor");
        }
    }
}
