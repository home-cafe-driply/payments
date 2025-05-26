package com.driply.payments.payment.gateway;

import java.util.Map;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.entity.PaymentError;
import com.driply.payments.payment.entity.PaymentStatus;

public interface PaymentGateway {
    // 결제 실행
    Map<String, Object> processPayment(PaymentRequestDTO requestDTO, long paymentId);

    // 결제 상태 조회
    PaymentStatus checkStatus(String transactionId);

    // 결제 취소
    boolean refundPayment(String transactionId);

    // 결제 수단 정보
    String getPGType();

    // 에러 정보
    PaymentError getLastError();
}
