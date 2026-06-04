package com.bcs.networkdevicemonitor.dto.response;

public record ApiError(String message, String code, String field) {

    public static ApiError of(String message, String code) {
        return new ApiError(message, code, null);
    }

    public static ApiError validation(String field, String message) {
        return new ApiError(message, "VALIDATION_ERROR", field);
    }
}
