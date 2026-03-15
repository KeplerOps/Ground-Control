package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.shared.web.ActorFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ActorFilterTest {

    private final ActorFilter filter = new ActorFilter();

    @Test
    void setsActorFromHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "test-user");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // After filter completes, ActorHolder and MDC should be cleared
        assertThat(ActorHolder.get()).isNull();
        assertThat(MDC.get("actor")).isNull();
    }

    @Test
    void defaultsToAnonymousWhenHeaderMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        // Capture the actor value during filter execution
        var chain = new MockFilterChain() {
            String capturedActor;

            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                capturedActor = ActorHolder.get();
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("anonymous");
    }

    @Test
    void defaultsToAnonymousWhenHeaderBlank() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "   ");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain() {
            String capturedActor;

            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                capturedActor = ActorHolder.get();
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("anonymous");
    }

    @Test
    void setsActorHolderAndMdcDuringRequest() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Actor", "alice");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain() {
            String capturedActor;
            String capturedMdc;

            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                capturedActor = ActorHolder.get();
                capturedMdc = MDC.get("actor");
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(chain.capturedActor).isEqualTo("alice");
        assertThat(chain.capturedMdc).isEqualTo("alice");
        // Cleared after
        assertThat(ActorHolder.get()).isNull();
        assertThat(MDC.get("actor")).isNull();
    }
}
