package com.music.song.exception;

public class ConflictException extends ApiException {

    public ConflictException(String errorMessage) {
        super(errorMessage, "409");
    }
}
