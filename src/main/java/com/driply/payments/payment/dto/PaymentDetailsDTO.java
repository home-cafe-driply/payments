package com.driply.payments.payment.dto;

import java.time.OffsetDateTime;

public abstract class PaymentDetailsDTO {
	public abstract String getStatus();

	public abstract String getTransactionId();

	public abstract String getPaymentMethod();

	public abstract OffsetDateTime getApprovedAt();
}
