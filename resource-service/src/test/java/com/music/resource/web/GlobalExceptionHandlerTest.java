package com.music.resource.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.music.resource.dto.error.ApiErrorResponse;
import com.music.resource.dto.error.ValidationErrorResponse;
import com.music.resource.exception.ApiException;
import com.music.resource.exception.BadRequestException;
import com.music.resource.exception.InternalServerErrorException;
import com.music.resource.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsBadRequestWithFieldErrorDetails() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("resource", "name", "must not be blank")));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorMessage()).isEqualTo("Validation error");
        assertThat(response.getBody().details()).containsEntry("name", "must not be blank");
        assertThat(response.getBody().errorCode()).isEqualTo("400");
    }

    @Test
    void handleBadRequest_returnsBadRequestWithMessageAndCode() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleBadRequest(new BadRequestException("invalid input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("invalid input", "400"));
    }

    @Test
    void handleTypeMismatch_returnsBadRequestWithIdMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", mock(MethodParameter.class), null);

        ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorMessage())
                .isEqualTo("Invalid value 'abc' for ID. Must be a positive integer");
    }

    @Test
    void handleTypeMismatch_nullValue_reportsNullInMessage() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException(null, Long.class, "id", mock(MethodParameter.class), null);

        ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getBody().errorMessage())
                .isEqualTo("Invalid value 'null' for ID. Must be a positive integer");
    }

    @Test
    void handleNotFound_returnsNotFoundWithMessageAndCode() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(new NotFoundException("Resource with ID=7 not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("Resource with ID=7 not found", "404"));
    }

    @Test
    void handleMissingParameter_returnsBadRequestWithCsvMessage() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleMissingParameter(new MissingServletRequestParameterException("id", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("CSV string format is invalid", "400"));
    }

    @Test
    void handleUnsupportedMediaType_returnsBadRequestWithMediaTypeMessage() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.valueOf("audio/mpeg")));

        ResponseEntity<ApiErrorResponse> response = handler.handleUnsupportedMediaType(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorMessage())
                .isEqualTo("Invalid file format: text/plain. Only MP3 files are allowed");
    }

    @Test
    void handleUnsupportedMediaType_nullContentType_reportsUnknownInMessage() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("Unsupported content type");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnsupportedMediaType(ex);

        assertThat(response.getBody().errorMessage())
                .isEqualTo("Invalid file format: unknown. Only MP3 files are allowed");
    }

    @Test
    void handleApiException_mappableErrorCode_usesItAsHttpStatus() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleApiException(new InternalServerErrorException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isEqualTo(new ApiErrorResponse("An error occurred on the server", "500"));
    }

    @Test
    void handleApiException_unmappableErrorCode_fallsBackToInternalServerError() {
        ApiException unmapped = new ApiException("boom", "999") {};

        ResponseEntity<ApiErrorResponse> response = handler.handleApiException(unmapped);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("boom", "999"));
    }

    @Test
    void handleUnexpected_returnsGenericInternalServerError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(new RuntimeException("kaboom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse("An error occurred on the server", "500"));
    }
}
