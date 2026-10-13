package com.kkamar.cnh.exception;

/** 게시판 페이지를 가져오지 못했을 때 (HTTP 오류 상태 또는 네트워크 오류). */
public class PageFetchException extends Exception {

    private final Integer statusCode;

    public PageFetchException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** HTTP 오류 응답이면 상태 코드, 네트워크 오류 등이면 null */
    public Integer getStatusCode() {
        return statusCode;
    }
}
