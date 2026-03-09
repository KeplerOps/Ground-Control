package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.domain.exception.AuthorizationException;
import com.keplerops.groundcontrol.domain.exception.GroundControlException;
import org.junit.jupiter.api.Test;

class ExceptionHierarchyTest {

    @Test
    void authenticationExceptionHasCorrectCode() {
        var ex = new AuthenticationException("bad creds");
        assertThat(ex.getErrorCode()).isEqualTo("authentication_error");
        assertThat(ex.getMessage()).isEqualTo("bad creds");
    }

    @Test
    void authorizationExceptionHasCorrectCode() {
        var ex = new AuthorizationException("forbidden");
        assertThat(ex.getErrorCode()).isEqualTo("authorization_error");
        assertThat(ex.getMessage()).isEqualTo("forbidden");
    }

    @Test
    void groundControlExceptionWithCause() {
        var cause = new RuntimeException("root cause");
        var ex = new GroundControlException("wrapped", "internal_error", cause);
        assertThat(ex.getErrorCode()).isEqualTo("internal_error");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
