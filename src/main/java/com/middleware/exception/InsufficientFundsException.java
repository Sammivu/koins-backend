package com.middleware.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
