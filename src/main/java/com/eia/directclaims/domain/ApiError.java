package com.eia.directclaims.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> details,
        String category
) {
    public static ApiError of(int status, String code, String message, String path, List<FieldViolation> details) {
        return of(status, code, message, path, details, null);
    }

    public static ApiError of(int status,
                              String code,
                              String message,
                              String path,
                              List<FieldViolation> details,
                              String category) {
        return new ApiError(Instant.now(), status, code, message, path, details, category);
    }
}
