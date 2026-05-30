package com.music.resource.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String errorMessage) {
        super(errorMessage, "400");
    }
}
