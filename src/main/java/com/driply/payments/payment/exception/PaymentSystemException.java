package com.driply.payments.payment.exception;

public class PaymentSystemException extends RuntimeException {
    private final String errorType;

    public PaymentSystemException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
}
