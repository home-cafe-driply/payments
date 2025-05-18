package com.driply.payments.payment.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {
    private final List<PaymentGateway> gateways;

    /**
     * pgType에 맞는 PaymentGateway 구현체 반환
     */
    public PaymentGateway getGateway(String pgType) {
        return gateways.stream()
                .filter(gateway -> gateway.getPGType().equalsIgnoreCase(pgType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported PG Type: " + pgType));
    }
}
