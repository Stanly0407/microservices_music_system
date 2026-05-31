package com.music.resource.exception;

public class InternalServerErrorException extends ApiException {

    public InternalServerErrorException() {
        super("An error occurred on the server", "500");
    }
}
