package com.kkamar.cnh.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/** 모든 오류 응답의 공통 형태 */
public record ApiError(int status, String error, String message, LocalDateTime timestamp) {

    public static ApiError of(int status, String message) {
        HttpStatus resolved = HttpStatus.resolve(status);
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new ApiError(status, reason, message, LocalDateTime.now());
    }
}
