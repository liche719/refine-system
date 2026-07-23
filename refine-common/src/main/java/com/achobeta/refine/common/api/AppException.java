package com.achobeta.refine.common.api;

public class AppException extends RuntimeException {
    private final int code;

    public AppException(String message) {
        this(-500, message);
    }

    public AppException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
