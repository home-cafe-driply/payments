package com.driply.payments.payment.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.driply.payments.payment.dto.PaymentResultDTO;

import reactor.core.publisher.Sinks;

/**
 * WebFlux 환경에서 SSE(Server-Sent Events) 연결을 관리하는 중앙 관리자 클래스.
 * 각 orderId에 해당하는 Sink를 보관하여, 어느 곳에서든 결제 결과 이벤트를 해당 클라이언트로 보낼 수 있도록 합니다.
 */
@Component
public class PaymentSseSinkManager {

	private final Map<String, Sinks.Many<PaymentResultDTO>> sinks = new ConcurrentHashMap<>();

	/**
	 * 지정된 orderId에 대한 Sink를 가져오거나 새로 생성합니다.
	 * computeIfAbsent를 사용하여 동시성 문제를 방지합니다.
	 * @param orderId 고유 주문 ID
	 * @return 해당 주문 ID에 대한 Sink
	 */
	public Sinks.Many<PaymentResultDTO> getOrCreateSink(String orderId) {
		return sinks.computeIfAbsent(orderId, key -> Sinks.many().multicast().onBackpressureBuffer());
	}

	/**
	 * 지정된 orderId의 Sink로 결제 결과 이벤트를 발생시킵니다.
	 * @param orderId 고유 주문 ID
	 * @param result 전송할 결제 결과 데이터
	 */
	public void emit(String orderId, PaymentResultDTO result) {
		Sinks.Many<PaymentResultDTO> sink = sinks.get(orderId);
		if (sink != null) {
			sink.tryEmitNext(result);
		}
	}

	/**
	 * 클라이언트 연결이 종료되거나 완료되었을 때, 해당 Sink를 제거하여 메모리 누수를 방지합니다.
	 * @param orderId 고유 주문 ID
	 */
	public void removeSink(String orderId) {
		sinks.remove(orderId);
	}
}
