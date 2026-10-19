package com.eia.directclaims.domain;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ClaimNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BookAccessDeniedException.class)
    public ResponseEntity<ApiError> bookDenied(BookAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Rejected non-DIRECT book access on {}", request.getRequestURI());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found", request, null);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiError> validation(DomainValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldViolation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Missing required header: " + ex.getHeaderName(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                "Invalid value for " + ex.getName(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed JSON request body", request, null);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> expertRejected(RestClientResponseException ex, HttpServletRequest request) {
        int upstream = ex.getStatusCode().value();
        if (upstream == 404) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found", request, null);
        }
        if (upstream == 400) {
            return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "The claims expert rejected the request", request, null);
        }
        if (upstream == 409) {
            return error(HttpStatus.CONFLICT, "CONFLICT",
                    "The claim was modified concurrently; reload and retry", request, null);
        }
        log.error("Claims expert returned {}", upstream);
        return error(HttpStatus.BAD_GATEWAY, "EXPERT_ERROR", "Claims expert error", request, null);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> expertUnavailable(RestClientException ex, HttpServletRequest request) {
        log.error("Claims expert is unavailable: {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "EXPERT_UNAVAILABLE",
                "Claims expert is unavailable", request, null);
    }

    private FieldViolation toViolation(FieldError fieldError) {
        return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status,
                                                  String code,
                                                  String message,
                                                  HttpServletRequest request,
                                                  List<FieldViolation> details) {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
