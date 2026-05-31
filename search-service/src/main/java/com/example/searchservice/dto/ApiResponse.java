package com.example.searchservice.dto;

import java.util.Map;

public record ApiResponse<T>(
        String status,
        String message,
        T data,
        Map<String, String> errors,
        String traceId
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, null, null);
    }

    public static <T> ApiResponse<T> failed(String message, String traceId) {
        return new ApiResponse<>("FAILED", message, null, null, traceId);
    }

    public static <T> ApiResponse<T> validationFailed(Map<String, String> errors) {
        return new ApiResponse<>("VALIDATION_FAILED", "Request validation failed", null, errors, null);
    }
}
