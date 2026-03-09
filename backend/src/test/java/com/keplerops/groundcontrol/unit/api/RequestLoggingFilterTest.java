package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.shared.logging.RequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
        assertThat(MDC.get("request_id")).isNull(); // cleared after filter
    }

    @Test
    void usesProvidedRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "test-id-123");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isEqualTo("test-id-123");
    }

    @Test
    void generatesIdForBlankHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "   ");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-ID")).isNotEqualTo("   ");
        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
    }
}
