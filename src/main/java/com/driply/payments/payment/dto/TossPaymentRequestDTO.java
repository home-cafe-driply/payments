package com.driply.payments.payment.dto;

import java.util.Collections;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TossPaymentRequestDTO extends PaymentRequestDTO {
	private String paymentKey;

	@Override
	public Map<String, Object> getModuleSpecificData() {
		if (paymentKey == null) {
			return Collections.emptyMap();
		}
		return Map.of(
			"paymentKey", paymentKey
		);
	}
}
