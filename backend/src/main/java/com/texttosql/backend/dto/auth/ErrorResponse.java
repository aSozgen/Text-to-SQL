package com.texttosql.backend.dto.auth;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, LocalDateTime.now(), path, null);
    }

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now(), null, null);
    }
}