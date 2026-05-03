package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.security.IpAllowlistFilter;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IpAllowlistFilterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private IpAllowlistFilter newFilter(SecurityProperties props) {
        var filter = new IpAllowlistFilter(props, mapper);
        filter.compile();
        return filter;
    }

    @Test
    void emptyAllowlist_passesThrough() throws Exception {
        var props = new SecurityProperties();
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void inRangeIpv4_passes() throws Exception {
        var props = new SecurityProperties();
        props.setIpAllowlist(List.of("10.0.0.0/8", "127.0.0.0/8"));
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.5.6.7");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void outOfRange_returns403WithErrorEnvelope() throws Exception {
        var props = new SecurityProperties();
        props.setIpAllowlist(List.of("10.0.0.0/8"));
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        var body = mapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo("access_denied");
        assertThat(body.path("error").path("message").asText()).isNotBlank();
    }

    @Test
    void ipv6_inRange_passes() throws Exception {
        var props = new SecurityProperties();
        props.setIpAllowlist(List.of("::1/128"));
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void securityDisabled_skipsAllowlist() throws Exception {
        var props = new SecurityProperties();
        props.setEnabled(false);
        props.setIpAllowlist(List.of("10.0.0.0/8"));
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotTrustForwardedForHeader() throws Exception {
        // X-Forwarded-For must NOT be honored by default; only the connection's remoteAddr counts.
        var props = new SecurityProperties();
        props.setIpAllowlist(List.of("10.0.0.0/8"));
        var filter = newFilter(props);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");
        request.addHeader("X-Forwarded-For", "10.0.0.5");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void compile_failsFastOnInvalidCidr() {
        var props = new SecurityProperties();
        props.setIpAllowlist(List.of("not-a-cidr"));
        var filter = new IpAllowlistFilter(props, mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(filter::compile)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ipAllowlist");
    }
}
