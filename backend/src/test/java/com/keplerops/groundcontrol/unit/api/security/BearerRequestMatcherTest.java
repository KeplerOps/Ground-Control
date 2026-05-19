package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.shared.security.BearerRequestMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The ADR-037 bearer/browser chain split rides entirely on this predicate. If it ever drifts
 * the wrong way the two filter chains either both fire on the same request (double-auth) or
 * neither does (deny-all by silent fall-through). The exhaustive cases below pin the contract.
 */
class BearerRequestMatcherTest {

    private final BearerRequestMatcher matcher = new BearerRequestMatcher();

    @Nested
    class Matches {

        @Test
        void bearerSchemeWithToken() {
            assertThat(matcher.matches(authHeader("Bearer abc123"))).isTrue();
        }

        @Test
        void bearerSchemeCaseInsensitive() {
            assertThat(matcher.matches(authHeader("bearer abc"))).isTrue();
            assertThat(matcher.matches(authHeader("BEARER abc"))).isTrue();
            assertThat(matcher.matches(authHeader("BeArEr abc"))).isTrue();
        }

        @Test
        void bearerSchemeWithEmptyTokenStillMatches() {
            // Wire-level matching is by scheme; BearerTokenAuthFilter is the one that
            // validates the token. The chain matcher's job is only to route the request.
            assertThat(matcher.matches(authHeader("Bearer "))).isTrue();
        }
    }

    @Nested
    class DoesNotMatch {

        @Test
        void noAuthorizationHeader() {
            assertThat(matcher.matches(new MockHttpServletRequest())).isFalse();
        }

        @Test
        void blankAuthorizationHeader() {
            assertThat(matcher.matches(authHeader(""))).isFalse();
            assertThat(matcher.matches(authHeader("   "))).isFalse();
        }

        @Test
        void basicScheme() {
            assertThat(matcher.matches(authHeader("Basic dXNlcjpwYXNz"))).isFalse();
        }

        @Test
        void digestScheme() {
            assertThat(matcher.matches(authHeader("Digest username=\"x\""))).isFalse();
        }

        @Test
        void bearerSubstringInOtherScheme() {
            // A header that contains "Bearer" later in the value (not at the start) is NOT
            // a bearer request; otherwise the matcher would mis-route hostile or weird traffic.
            assertThat(matcher.matches(authHeader("X-Bearer abc"))).isFalse();
        }

        @Test
        void sessionCookieOnly() {
            var req = new MockHttpServletRequest();
            req.setCookies(new jakarta.servlet.http.Cookie("GC_SESSION", "abc"));
            assertThat(matcher.matches(req)).isFalse();
        }
    }

    private static MockHttpServletRequest authHeader(String value) {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", value);
        return req;
    }
}
