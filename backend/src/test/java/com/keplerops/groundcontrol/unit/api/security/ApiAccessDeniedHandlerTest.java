package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.security.ApiAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class ApiAccessDeniedHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(mapper);

    @Test
    void emits403WithErrorEnvelope() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        var body = mapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo("access_denied");
        assertThat(body.path("error").path("message").asText()).isNotBlank();
    }

    @Test
    void doesNotLeakExceptionMessage_inResponseBody() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("internal:user-table:secret"));

        assertThat(response.getContentAsString()).doesNotContain("internal:user-table");
    }
}
