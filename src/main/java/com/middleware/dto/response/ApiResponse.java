package com.middleware.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }
}
