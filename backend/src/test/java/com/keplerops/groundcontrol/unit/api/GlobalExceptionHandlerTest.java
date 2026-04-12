package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.ErrorResponse;
import com.keplerops.groundcontrol.api.GlobalExceptionHandler;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
    void handleMethodNotSupported_returnsMethodNotAllowed() {
        var ex = new HttpRequestMethodNotSupportedException("POST");

        var response = handler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("method_not_allowed");
        assertThat(response.getBody().error().message()).isEqualTo("Request method 'POST' is not supported");
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

    @Test
    void handleConflict_omitsDetailWhenExceptionDetailIsEmpty() {
        var ex = new ConflictException("Already exists");

        var response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        // Empty detail must be omitted from the wire envelope, not serialized as `{}`,
        // so legacy single-arg ConflictException sites do not regress after the
        // detail-aware envelope was added.
        assertThat(response.getBody().error().detail()).isNull();
    }

    @Test
    void handleConflict_includesDetailWhenExceptionCarriesDetail() {
        Map<String, Serializable> detail = new LinkedHashMap<>();
        detail.put("threatModelUid", "TM-001");
        detail.put("assetUids", new java.util.ArrayList<>(List.of("ASSET-001")));
        var ex = new ConflictException("Threat model referenced", "threat_model_referenced", detail);

        var response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("threat_model_referenced");
        assertThat(response.getBody().error().detail()).containsEntry("threatModelUid", "TM-001");
        assertThat(response.getBody().error().detail()).containsKey("assetUids");
    }

    @Test
    void handleValidation_omitsDetailWhenExceptionDetailIsEmpty() {
        var ex = new DomainValidationException("Title must not be blank");

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        // Same envelope contract as handleConflict: empty detail must be omitted
        // so legacy single-arg DomainValidationException sites do not serialize
        // `detail: {}` for the much larger 422 surface.
        assertThat(response.getBody().error().detail()).isNull();
    }

    @Test
    void handleValidation_includesDetailWhenExceptionCarriesDetail() {
        Map<String, Serializable> detail = new LinkedHashMap<>();
        detail.put("field", "title");
        var ex = new DomainValidationException("Title must not be blank", "validation_error", detail);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("validation_error");
        assertThat(response.getBody().error().detail()).containsEntry("field", "title");
    }
}
