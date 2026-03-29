package com.keplerops.groundcontrol.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.infrastructure.web.SpaForwardingFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpaForwardingFilterTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/requirements",
                "/graph",
                "/p/aptl",
                "/p/aptl/graph",
                "/p/aptl/requirements/some-id",
                "/p/aptl/requirements/some-id/sub/deep",
                "/a/b/c/d/e/f/g/h/i/j"
            })
    void spaRoutes_shouldForward(String path) {
        assertThat(SpaForwardingFilter.shouldForwardToSpa(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/",
                "/api/projects",
                "/api/projects/123/requirements",
                "/actuator/health",
                "/actuator/info",
                "/assets/main.js",
                "/favicon.ico",
                "/index.html"
            })
    void excludedPaths_shouldNotForward(String path) {
        assertThat(SpaForwardingFilter.shouldForwardToSpa(path)).isFalse();
    }

    @Test
    void nullPath_shouldNotForward() {
        assertThat(SpaForwardingFilter.shouldForwardToSpa(null)).isFalse();
    }

    @Test
    void emptyPath_shouldNotForward() {
        assertThat(SpaForwardingFilter.shouldForwardToSpa("")).isFalse();
    }
}
