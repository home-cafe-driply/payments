package com.driply.payments.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TossPaymentDetailsDTO extends PaymentDetailsDTO {
	String mId;
	String lastTransactionKey;
	String paymentKey;
	String orderId;
	String orderName;
	long taxExemptionAmount;
	String status;
	OffsetDateTime requestedAt;
	OffsetDateTime approvedAt;
	boolean useEscrow;
	boolean cultureExpense;
	String card;
	String virtualAccount;
	String transfer;
	String mobilePhone;
	String giftCertificate;
	String cashReceipt;
	String cashReceipts;
	String discount;
	String cancels;
	String secret;
	String type;
	Map<String, Object> easyPay;
	String country;
	String failure;
	String isPartialCancelable;
	Map<String, Object> receipt;
	Map<String, Object> checkout;
	String currency;
	BigDecimal totalAmount;
	BigDecimal balanceAmount;
	BigDecimal suppliedAmount;
	BigDecimal vat;
	BigDecimal taxFreeAmount;
	String method;
	String version;
	String metadata;

	@Override
	public String getTransactionId() {
		return this.lastTransactionKey;
	}

	@Override
	public String getPaymentMethod() {
		return this.method;
	}
}
