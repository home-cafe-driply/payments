package com.driply.payments.payment.exception;

import com.driply.payments.payment.entity.PGType;

import java.math.BigDecimal;

public class PaymentException extends Exception {
    private final BigDecimal amount; // 결제 금액
    private final String transactionId; // 거래 ID
    private final PGType pgType; // 결제 수단

    public PaymentException(
            PaymentErrorCode errorCode,
            String message,
            BigDecimal amount,
            String transactionId,
            PGType pgType
    ) {
        super(message);
        this.amount = amount;
        this.transactionId = transactionId;
        this.pgType = pgType;
    }
}
