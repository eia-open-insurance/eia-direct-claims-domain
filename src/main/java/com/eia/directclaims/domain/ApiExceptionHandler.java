package com.eia.directclaims.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private final ObjectMapper objectMapper;

    public ApiExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ClaimNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AllocationNotFoundException.class)
    public ResponseEntity<ApiError> allocationNotFound(AllocationNotFoundException ex,
                                                       HttpServletRequest request) {
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
        String detail = expertDetail(ex);
        if (upstream == 404) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND",
                    detail == null ? "Not found" : detail, request, null);
        }
        if (upstream == 400) {
            return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    detail == null ? "The claims expert rejected the request" : detail, request, null);
        }
        if (upstream == 409) {
            if ("EIA-DIR-4091".equals(expertCode(ex))) {
                return error(HttpStatus.CONFLICT, "EIA-DIR-4091",
                        detail == null ? "Idempotency-Key was already used with a different request."
                                : detail,
                        request, null, "IDEMPOTENCY_KEY_REUSED");
            }
            return error(HttpStatus.CONFLICT, "CONFLICT",
                    detail == null ? "The claim was modified concurrently; reload and retry" : detail,
                    request, null);
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

    /**
     * Preserves the expert's problem detail so a handler can see why the task or
     * note write was rejected. The domain keeps its own status and code mapping.
     */
    private String expertDetail(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode problem = objectMapper.readTree(body);
            JsonNode detail = problem.get("detail");
            if (detail == null || !detail.isTextual()) {
                detail = problem.get("message");
            }
            if (detail != null && detail.isTextual() && !detail.asText().isBlank()) {
                return detail.asText();
            }
        } catch (JsonProcessingException ignored) {
            // A non-JSON expert error keeps the domain's generic message.
        }
        return null;
    }

    /** Reads the stable expert error code so contract-specific problems survive. */
    private String expertCode(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode problem = objectMapper.readTree(body);
            JsonNode code = problem.get("code");
            return code != null && code.isTextual() ? code.asText() : null;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private FieldViolation toViolation(FieldError fieldError) {
        return new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status,
                                                  String code,
                                                  String message,
                                                  HttpServletRequest request,
                                                  List<FieldViolation> details) {
        return error(status, code, message, request, details, null);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status,
                                                  String code,
                                                  String message,
                                                  HttpServletRequest request,
                                                  List<FieldViolation> details,
                                                  String category) {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI(), details,
                category);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
