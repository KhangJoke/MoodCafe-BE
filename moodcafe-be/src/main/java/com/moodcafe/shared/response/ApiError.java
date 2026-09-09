package com.moodcafe.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path,
        Long retryAfterSeconds
) {
    public ApiError(
            Instant timestamp,
            int status,
            String code,
            String message,
            Map<String, String> fieldErrors,
            String path
    ) {
        this(timestamp, status, code, message, fieldErrors, path, null);
    }
}

