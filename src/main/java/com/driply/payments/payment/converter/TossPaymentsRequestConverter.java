package com.driply.payments.payment.converter;

import com.driply.payments.payment.dto.TossPaymentRequestDTO;
import com.driply.payments.payment.entity.PGType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

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
                // TODO: 하드 코딩 제거 필요
                .requestUri("confirm/widget")
                .build();
    }
}
