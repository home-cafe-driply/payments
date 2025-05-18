package com.driply.payments.payment.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentError {
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
}
