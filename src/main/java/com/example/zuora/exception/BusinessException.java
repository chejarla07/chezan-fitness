package com.example.zuora.exception;

/**
 * Exception for business logic errors that should be shown to the user.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}