package com.music.resource.exception;

public abstract class ApiException extends RuntimeException {

    private final String errorCode;

    protected ApiException(String errorMessage, String errorCode) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
