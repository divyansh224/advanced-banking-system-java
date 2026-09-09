package com.bankapp.banking.exception;

/** Thrown when an admin tries to approve/reject a transaction that isn't actually pending fraud review. */
public class FraudReviewException extends RuntimeException {
    public FraudReviewException(String message) {
        super(message);
    }
}
