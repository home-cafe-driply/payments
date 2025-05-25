package com.driply.payments.payment.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentRequestDTO extends PaymentRequestDTO {
    private String paymentKey;
    private String requestUri;

    @Override
    public Map<String, Object> getModuleSpecificData() {
        return Map.of(
                "paymentKey", paymentKey,
                "requestUri", requestUri
        );
    }
}
