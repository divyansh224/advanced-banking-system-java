package com.bankapp.banking.exception;

public class AccessDeniedExceptionCustom extends RuntimeException {
    public AccessDeniedExceptionCustom(String message) {
        super(message);
    }
}
