package com.kkamar.cnh.exception;

/** 권한이 없는 요청 (관리자 토큰 불일치 등) -> 403 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
