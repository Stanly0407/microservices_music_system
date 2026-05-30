package com.music.song.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String errorMessage) {
        super(errorMessage, "400");
    }
}
