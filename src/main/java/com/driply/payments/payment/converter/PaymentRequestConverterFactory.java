package com.driply.payments.payment.converter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.driply.payments.payment.dto.PaymentRequestDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRequestConverterFactory {

    private final List<PaymentRequestConverter<?>> converters;

    public PaymentRequestDTO convert(Map<String, Object> request) {
        String pgType = getPgType(request);
        return converters.stream()
                .filter(converter -> converter.getPgType().equalsIgnoreCase(pgType))
                .findFirst()
                .map(converter -> converter.convert(request))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported PG Type: " + pgType));
    }

    private String getPgType(Map<String, Object> request) {
        if (request.containsKey("paymentKey")) {
            return "TOSS";
        }
        return "NONE";
    }
}