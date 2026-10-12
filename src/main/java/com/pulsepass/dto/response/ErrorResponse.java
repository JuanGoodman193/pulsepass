package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
    public static ErrorResponse of(int status, String error, String message, Map<String, String> details) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, details != null ? details : Map.of());
    }

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, Map.of());
    }
}
