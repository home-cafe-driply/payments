package com.driply.payments.payment.converter;

import com.driply.payments.payment.dto.PaymentRequestDTO;

import java.util.Map;

public interface PaymentRequestConverter<T extends PaymentRequestDTO> {
    /**
     * PG사 타입 식별자 (예: "TOSS", "PORTONE")
     */
    String getPgType();

    /**
     * Map 형태의 요청 데이터를 PG사별 DTO로 변환
     */
    T convert(Map<String, Object> requestData);
}
