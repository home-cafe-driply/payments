package com.driply.payments.payment.gateway;

import com.driply.payments.payment.dto.PaymentQuery;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.TossPaymentDetailsDTO;
import com.driply.payments.payment.entity.PaymentError;
import com.driply.payments.payment.entity.PaymentStatus;

import reactor.core.publisher.Mono;

public interface PaymentGateway {
	// 결제 실행
	void processPayment(PaymentRequestDTO requestDTO, long paymentId);

	// 결제 상세 조회
	Mono<TossPaymentDetailsDTO> queryPayment(PaymentQuery query);

	// 결제 상태 조회
	PaymentStatus checkStatus(String transactionId);

	// 결제 취소
	boolean refundPayment(String transactionId);

	// 결제 수단 정보
	String getPGType();

	// 에러 정보
	PaymentError getLastError();
}
