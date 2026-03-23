package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.ErrorResponse;
import com.keplerops.groundcontrol.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleHttpMessageNotReadable_returnsBadRequest() {
        var ex = new HttpMessageNotReadableException("bad json");

        var response = handler.handleHttpMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("bad_request");
        assertThat(response.getBody().error().message()).isEqualTo("Malformed request body");
    }

    @Test
    void handleMethodArgumentTypeMismatch_returnsBadRequest() {
        var ex = new MethodArgumentTypeMismatchException("abc", String.class, "id", null, null);

        var response = handler.handleMethodArgumentTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("bad_request");
        assertThat(response.getBody().error().message()).isEqualTo("Invalid value for parameter 'id'");
    }

    @Test
    void handleMissingServletRequestParameter_returnsBadRequest() {
        var ex = new MissingServletRequestParameterException("page", "int");

        var response = handler.handleMissingServletRequestParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("bad_request");
        assertThat(response.getBody().error().message()).isEqualTo("Missing required parameter 'page'");
    }

    @Test
    void handleMissingServletRequestPart_returnsBadRequest() {
        var ex = new MissingServletRequestPartException("file");

        var response = handler.handleMissingServletRequestPart(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("bad_request");
        assertThat(response.getBody().error().message()).isEqualTo("Missing required part 'file'");
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        var ex = new RuntimeException("something broke");

        var response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("internal_error");
        assertThat(response.getBody().error().message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void allHandlers_returnConsistentErrorEnvelope() {
        ErrorResponse r1 = handler.handleHttpMessageNotReadable(new HttpMessageNotReadableException("x"))
                .getBody();
        ErrorResponse r2 = handler.handleGeneric(new RuntimeException("x")).getBody();

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        assertThat(r1.error().code()).isNotBlank();
        assertThat(r2.error().code()).isNotBlank();
        assertThat(r1.error().message()).isNotBlank();
        assertThat(r2.error().message()).isNotBlank();
    }
}
