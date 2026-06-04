package com.bcs.networkdevicemonitor.dto.response;

import java.util.List;

public record ApiResponse<T>(T data, List<ApiError> errors, ApiMeta meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, ApiMeta.of());
    }

    public static <T> ApiResponse<T> success(T data, int count) {
        return new ApiResponse<>(data, null, ApiMeta.of(count));
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(null, List.of(error), ApiMeta.of());
    }

    public static <T> ApiResponse<T> errors(List<ApiError> errors) {
        return new ApiResponse<>(null, errors, ApiMeta.of());
    }
}
