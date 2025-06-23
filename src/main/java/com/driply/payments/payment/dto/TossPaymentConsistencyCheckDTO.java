package com.driply.payments.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TossPaymentConsistencyCheckDTO extends PaymentConsistencyCheckDTO {
	private String paymentKey;
	private String orderId;
	private String orderName;
	private OffsetDateTime requestedAt;
	private OffsetDateTime approvedAt;
	private String secret;
	private String status;
	private BigDecimal totalAmount;
}
