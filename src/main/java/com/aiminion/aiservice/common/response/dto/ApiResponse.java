package com.aiminion.aiservice.common.response.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
public class ApiResponse<T> {

    private int success;
    private int code;
    private Map<String, Object> meta;
    private T data;
    private String message;

    // ✅ SUCCESS RESPONSE
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(1)
                .code(200)
                .data(data)
                .message(message)
                .build();
    }

    // ✅ FAIL RESPONSE
    public static <T> ApiResponse<T> fail(int code, String message, Object data) {
        return ApiResponse.<T>builder()
                .success(0)
                .code(code)
                .data((T) data)
                .message(message)
                .build();
    }
}