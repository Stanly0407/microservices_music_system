package com.music.resource.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String errorMessage) {
        super(errorMessage, "404");
    }
}
