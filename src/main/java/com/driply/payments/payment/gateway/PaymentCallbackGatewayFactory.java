package com.driply.payments.payment.gateway;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentCallbackGatewayFactory {
	private final List<PaymentCallbackGateway> gateways;

	/**
	 * pgType에 맞는 PaymentCallbackGateway 구현체 반환
	 */
	public PaymentCallbackGateway getGateway(String pgType) {
		return gateways.stream()
			.filter(gateway -> gateway.getPgName().equalsIgnoreCase(pgType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("PG 타입에 대한 게이트웨이를 찾을 수 없습니다: " + pgType));
	}
}
