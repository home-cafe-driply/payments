package com.driply.payments.payment.dto;

import com.driply.payments.payment.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultDTO {
	private Long paymentId;
	private PaymentStatus status;
	private String message;
	private boolean success;
}
