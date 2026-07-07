package com.music.processor.exception;

public class Mp3ProcessingException extends RuntimeException {

    public Mp3ProcessingException(String message) {
        super(message);
    }

    public Mp3ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
