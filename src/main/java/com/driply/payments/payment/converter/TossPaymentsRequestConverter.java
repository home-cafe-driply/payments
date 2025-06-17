package com.driply.payments.payment.converter;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.driply.payments.payment.dto.TossPaymentRequestDTO;
import com.driply.payments.payment.entity.PGType;

@Component
public class TossPaymentsRequestConverter implements PaymentRequestConverter<TossPaymentRequestDTO> {

	@Override
	public String getPgType() {
		return PGType.TOSS.name();
	}

	@Override
	public TossPaymentRequestDTO convert(Map<String, Object> requestData) {
		return TossPaymentRequestDTO.builder()
			.pgType(PGType.TOSS.name())
			.amount(new BigDecimal(requestData.get("amount").toString()))
			.orderId(requestData.get("orderId").toString())
			.paymentKey(requestData.get("paymentKey").toString())
			.build();
	}
}
