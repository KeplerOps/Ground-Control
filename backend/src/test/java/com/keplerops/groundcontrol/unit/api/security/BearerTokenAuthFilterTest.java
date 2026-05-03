package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.keplerops.groundcontrol.shared.security.BearerTokenAuthFilter;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.ApiCredential;
import com.keplerops.groundcontrol.shared.security.SecurityProperties.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class BearerTokenAuthFilterTest {

    private SecurityProperties props;
    private BearerTokenAuthFilter filter;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        props = new SecurityProperties();
        var user = new ApiCredential();
        user.setPrincipalName("alice");
        user.setToken("user-token-aaa");
        user.setRole(Role.USER);
        var admin = new ApiCredential();
        admin.setPrincipalName("bob");
        admin.setToken("admin-token-bbb");
        admin.setRole(Role.ADMIN);
        props.setCredentials(List.of(user, admin));
        filter = new BearerTokenAuthFilter(props);
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validUserToken_authenticatesWithRoleUser() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token-aaa");

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void validAdminToken_authenticatesWithRoleAdmin() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token-bbb");

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("bob");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("rejectedAuthorizationHeaders")
    void unauthorizedHeader_leavesContextEmpty_andContinuesChain(String description, String headerValue)
            throws Exception {
        var request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader("Authorization", headerValue);
        }

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    static Stream<Arguments> rejectedAuthorizationHeaders() {
        return Stream.of(
                Arguments.of("no header at all", null),
                Arguments.of("wrong scheme (Basic)", "Basic dXNlcjpwYXNz"),
                Arguments.of("Bearer with unknown token", "Bearer wrong-token"),
                Arguments.of("Bearer with empty token", "Bearer "));
    }

    @Test
    void caseInsensitiveScheme_isAccepted() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer user-token-aaa");

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
    }

    @Test
    void sameLengthWrongToken_doesNotAuthenticate() throws Exception {
        // user-token-aaa is 14 chars; provide a same-length wrong token to confirm the constant-time
        // compare fails closed when lengths match.
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token-XYZ");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void securityDisabled_skipsAuthentication() throws Exception {
        props.setEnabled(false);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token-aaa");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void chainIsCalledExactlyOnce_evenOnInvalidToken() throws Exception {
        var request = mock(HttpServletRequest.class);
        var resp = mock(HttpServletResponse.class);
        org.mockito.Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer nope");
        var localChain = mock(FilterChain.class);

        filter.doFilter(request, resp, localChain);

        verify(localChain, times(1)).doFilter(request, resp);
    }
}
