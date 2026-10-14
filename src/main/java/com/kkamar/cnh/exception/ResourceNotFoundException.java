package com.kkamar.cnh.exception;

/** 요청이 가리키는 리소스(디바이스, 공지, 학과 등)가 존재하지 않을 때 -> 404 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
