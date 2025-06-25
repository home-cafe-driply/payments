package com.driply.payments.payment.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.TossWebhookEventType;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.exception.PaymentCallbackException;
import com.driply.payments.payment.exception.PaymentInconsistencyException;
import com.driply.payments.payment.gateway.PaymentCallbackGateway;
import com.driply.payments.payment.gateway.PaymentCallbackGatewayFactory;
import com.driply.payments.payment.service.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackHandler {
	private final ObjectMapper objectMapper;
	private final PaymentCallbackGatewayFactory callbackGatewayFactory;
	private final PaymentService paymentService;

	/**
	 * Kafka "payment-callback" 토픽에서 결제 콜백 메시지를 처리합니다.
	 * <p>
	 * 메시지 유효성 검사, PG 타입 판별, 결제 정보 파싱 및 상태 업데이트를 수행합니다.
	 * 처리 중 오류 발생 시 {@link PaymentCallbackException}을 던지며 트랜잭션이 롤백됩니다.
	 *
	 * @param callbackMessage Kafka에서 수신한 콜백 메시지 (빈 값일 수 없음)
	 * @throws PaymentCallbackException 메시지 오류 또는 결제 정보 미존재 시 발생
	 */
	@Transactional
	@KafkaListener(
		topics = "payment-callback",
		groupId = "payment-callback-group",
		containerFactory = "kafkaListenerContainerFactory"
	)
	public void handleCallbackEvent(String callbackMessage) {
		if (!StringUtils.hasText(callbackMessage)) {
			throw new PaymentCallbackException("콜백 메시지가 비어있습니다.");
		}

		try {
			Map<String, Object> callbackData = objectMapper.readValue(callbackMessage, new TypeReference<>() {
			});
			PGType pgType = determinePGType(callbackData);

			PaymentCallbackGateway callbackGateway = callbackGatewayFactory.getGateway(pgType.name());
			PaymentDetailsDTO paymentDetailsDTO = callbackGateway.parsePaymentDetails(callbackData);
			callbackGateway.validateStatusChange(paymentDetailsDTO);
			Payment payment = callbackGateway.findPayment(paymentDetailsDTO);
			if (payment == null) {
				throw new PaymentCallbackException("결제 정보를 찾을 수 없습니다.");
			}

			paymentService.updatePaymentStatus(payment, paymentDetailsDTO);
		} catch (PaymentInconsistencyException e) {
			log.error("결제 콜백 중 오류 발생: {}", e.getMessage());
			throw new PaymentCallbackException("결제 콜백 중 에러 발생: " + e.getMessage());

		} catch (Exception e) {
			log.error("예상치 못한 예외 발생: {}", e.getMessage());
		}
	}

	/**
	 * 콜백 데이터에서 PG(Payment Gateway) 타입을 판별합니다.
	 * <p>
	 * PG사가 추가될 경우 if문에 조건을 추가하여 확장할 수 있습니다.
	 * 현재는 "eventType" 값이 TossWebhookEventType에 포함되면 {@link PGType#TOSS}를 반환합니다.
	 * 조건에 맞지 않으면 예외를 발생시킵니다.
	 *
	 * @param callbackData 콜백 메시지 파싱 결과
	 * @return 판별된 PG 타입
	 * @throws PaymentCallbackException 알 수 없는 PG 타입인 경우
	 */
	private PGType determinePGType(Map<String, Object> callbackData) {
		if (callbackData.containsKey("eventType")) {
			for (TossWebhookEventType eventType : TossWebhookEventType.values()) {
				if (eventType.name().equals(callbackData.get("eventType"))) {
					return PGType.TOSS;
				}
			}
		}
		throw new PaymentCallbackException("알 수 없는 PG 타입 callbackData: " + callbackData);
	}
}
