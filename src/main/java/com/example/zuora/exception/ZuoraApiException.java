package com.example.zuora.exception;

/**
 * Exception for Zuora API errors.
 */
public class ZuoraApiException extends RuntimeException {

    public ZuoraApiException(String message) {
        super(message);
    }

    public ZuoraApiException(String message, Throwable cause) {
        super(message, cause);
    }
}