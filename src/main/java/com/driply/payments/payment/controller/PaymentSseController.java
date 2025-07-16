package com.driply.payments.payment.controller;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.driply.payments.payment.dto.PaymentResultDTO;
import com.driply.payments.payment.service.PaymentSseSinkManager;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentSseController {

	private final PaymentSseSinkManager sseSinkManager;

	/**
	 * 클라이언트가 특정 주문 ID에 대한 결제 결과 알림을 구독하는 엔드포인트입니다.
	 * @param orderId 구독할 주문의 고유 ID
	 * @return 결제 결과 이벤트를 포함하는 Server-Sent Event 스트림
	 */
	@GetMapping(value = "/subscribe/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<PaymentResultDTO>> subscribe(@PathVariable String orderId) {
		Sinks.Many<PaymentResultDTO> sink = sseSinkManager.getOrCreateSink(orderId);

		// 30초마다 heart-beat 이벤트를 보내 연결이 끊어지는 것을 방지합니다.
		Flux<ServerSentEvent<PaymentResultDTO>> heartbeat = Flux.interval(Duration.ofSeconds(30))
			.map(seq -> ServerSentEvent.<PaymentResultDTO>builder()
				.comment("heartbeat")
				.build());

		return Flux.merge(sink.asFlux().map(result -> ServerSentEvent.builder(result).build()), heartbeat)
			.doOnCancel(() -> {
				// 클라이언트가 연결을 종료하면(예: 브라우저 창 닫기) Sink를 제거합니다.
				sseSinkManager.removeSink(orderId);
			});
	}
}
