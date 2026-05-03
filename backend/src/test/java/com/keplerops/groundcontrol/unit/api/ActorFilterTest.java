package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import com.keplerops.groundcontrol.shared.web.ActorFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ActorFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static ActorFilter newFilter(boolean securityEnabled) {
        var props = new SecurityProperties();
        props.setEnabled(securityEnabled);
        return new ActorFilter(new TestObjectProvider<>(props));
    }

    private static final class TestObjectProvider<T> implements org.springframework.beans.factory.ObjectProvider<T> {
        private final T value;

        TestObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }
    }

    @Nested
    class WhenSecurityEnabled {

        private final ActorFilter filter = newFilter(true);

        @Test
        void anonymousRoute_doesNotHonorXActorHeader() throws Exception {
            // Production: X-Actor on a permitted anonymous route must NOT spoof identity.
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "spoofed");
            var response = new MockHttpServletResponse();
            var chain = capturingChain();

            filter.doFilter(request, response, chain);

            assertThat(chain.capturedActor).isEqualTo("anonymous");
        }

        @Test
        void anonymousRoute_withNoHeader_isAnonymous() throws Exception {
            var chain = capturingChain();

            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("anonymous");
        }

        @Test
        void authenticatedPrincipal_isUsed() throws Exception {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "bob", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var chain = capturingChain();

            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("bob");
        }

        @Test
        void authenticatedPrincipal_winsOverXActorHeader() throws Exception {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "bob", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "spoofed");
            var chain = capturingChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("bob");
        }

        @Test
        void clearsActorAfterFilterChain() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "test-user");

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            assertThat(ActorHolder.get()).isNull();
            assertThat(MDC.get("actor")).isNull();
        }
    }

    @Nested
    class WhenSecurityDisabled {

        private final ActorFilter filter = newFilter(false);

        @Test
        void honorsXActorHeader_whenNoAuthentication() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "alice");
            var chain = capturingChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("alice");
            assertThat(chain.capturedMdc).isEqualTo("alice");
        }

        @Test
        void defaultsToAnonymousWhenHeaderMissing() throws Exception {
            var chain = capturingChain();

            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("anonymous");
        }

        @Test
        void defaultsToAnonymousWhenHeaderBlank() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "   ");
            var chain = capturingChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("anonymous");
        }

        @Test
        void unauthenticatedTokenIsIgnored_andHeaderWins() throws Exception {
            var auth = new UsernamePasswordAuthenticationToken("dave", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            var request = new MockHttpServletRequest();
            request.addHeader("X-Actor", "fallback-user");
            var chain = capturingChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("fallback-user");
        }

        @Test
        void authenticatedPrincipalIsUsed_evenWhenSecurityDisabled() throws Exception {
            // If something (e.g. dev tooling) populated the SecurityContext, prefer it.
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "carol", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var chain = capturingChain();

            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

            assertThat(chain.capturedActor).isEqualTo("carol");
        }
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
