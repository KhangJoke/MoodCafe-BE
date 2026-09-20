package com.moodcafe.shared.response;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private int code = 200;

    private String message;

    @JsonAlias({"result"})
    private T data;

    @JsonProperty("result")
    public T getResult() {
        return data;
    }

    // Optional error fields
    private String errorCode;
    private Instant timestamp;
    private String path;

    // Helper methods for Success
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .data(data)
                .message(message)
                .build();
    }

    // Helper methods for Failure
    public static <T> ApiResponse<T> failed(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> failed(int code, String message, String errorCode, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}
