package com.driply.payments.payment.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentError {
	private String errorCode;
	private String message;
	private LocalDateTime timestamp;
}
