package com.driply.payments.payment.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TossPaymentCallbackRequestDTO extends PaymentCallbackRequestDTO {
	private LocalDateTime createdAt;
	private String eventType;
	private TossPaymentDetailsDTO data;
}
