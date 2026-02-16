package com.chatapp.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ApiResponse success(String message) {
        return ApiResponse.builder().success(true).message(message).build();
    }
    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder().success(true).message(message).data(data).build();
    }
    public static ApiResponse error(String message) {
        return ApiResponse.builder().success(false).message(message).build();
    }
}
