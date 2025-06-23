package com.driply.payments.payment.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.PaymentQuery;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResultDTO;
import com.driply.payments.payment.dto.TossPaymentCallbackRequestDTO;
import com.driply.payments.payment.dto.TossPaymentConsistencyCheckDTO;
import com.driply.payments.payment.dto.TossPaymentDetailsDTO;
import com.driply.payments.payment.dto.TossWebhookEventType;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.entity.PaymentStatus;
import com.driply.payments.payment.exception.NoSuchStatusException;
import com.driply.payments.payment.exception.PaymentCallbackException;
import com.driply.payments.payment.exception.PaymentInconsistencyException;
import com.driply.payments.payment.gateway.PaymentGateway;
import com.driply.payments.payment.gateway.PaymentGatewayFactory;
import com.driply.payments.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final PaymentGatewayFactory paymentGatewayFactory;
	private final PaymentRepository paymentRepository;
	private final ExecutorService paymentExecutor = Executors.newFixedThreadPool(20);
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public Payment getPaymentById(Long paymentId) {
		return paymentRepository.findById(paymentId)
			.orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다: " + paymentId));
	}

	/**
	 * 결제 승인 프로세스를 비동기로 처리합니다.
	 * @param requestDTO 결제 요청에 필요한 메타 데이터를 포함합니다.
	 * @return paymentId(접수된 결제 엔티티의 pk), status(PENDING 상태의 결제 엔티티 생성), message(결제 접수 응답 메시지), isSuccess(결제 접수 성공 여부)
	 */
	@Override
	@Transactional
	public PaymentResultDTO processPaymentAsync(PaymentRequestDTO requestDTO) {
		Payment payment = createPendingPayment(requestDTO);
		Payment savedPayment = paymentRepository.saveAndFlush(payment);
		PaymentGateway paymentGateway = paymentGatewayFactory.getGateway(requestDTO.getPgType());
		Long paymentId = savedPayment.getPaymentId();

		try {
			CompletableFuture.runAsync(() -> paymentGateway.processPayment(requestDTO, paymentId), paymentExecutor);

			return PaymentResultDTO.builder()
				.paymentId(savedPayment.getPaymentId())
				.status(savedPayment.getStatus())
				.success(true)
				.message("결제 요청이 접수되었습니다.")
				.build();
		} catch (RuntimeException e) {
			logger.error("비동기 결제 처리 중 예외 발생", e);

			return PaymentResultDTO.builder()
				.paymentId(null)
				.status(PaymentStatus.FAILED)
				.success(false)
				.message(e.getMessage())
				.build();
		}
	}

	@Transactional
	@KafkaListener(
		topics = "payment-callback",
		groupId = "payment-service-group",
		containerFactory = "kafkaListenerContainerFactory"
	)
	public void handleCallbackEvent(String callbackMessage) {
		if (!StringUtils.hasText(callbackMessage)) {
			throw new PaymentCallbackException("콜백 메시지가 비어있습니다.");
		}

		PGType pgType = PGType.NONE;
		PaymentDetailsDTO paymentDetails = null;
		Payment payment = null;
		try {
			if (callbackMessage.contains("tosspayments")) {
				pgType = PGType.TOSS;
				TossPaymentCallbackRequestDTO callbackData = objectMapper.readValue(callbackMessage,
					TossPaymentCallbackRequestDTO.class);
				logger.info("callbackData: {}", callbackData);

				paymentDetails = callbackData.getData();
				TossPaymentDetailsDTO tossDetails = (TossPaymentDetailsDTO)paymentDetails;

				payment = paymentRepository.findByPaymentKey(tossDetails.getPaymentKey()).orElse(null);
			}

			if (!validateStatusChange(pgType, paymentDetails)) {
				throw new PaymentCallbackException("");
			}

		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			return;
		}

		if (pgType == PGType.NONE) {
			logger.error("지원하지 않는 PG 타입입니다");
			return;
		}

		if (payment == null) {
			logger.error("결제 데이터를 찾을 수 없습니다.");
			return;
		}

		long paymentId = payment.getPaymentId();
		updatePaymentStatus(paymentId, paymentDetails);
	}

	@Override
	public boolean validateStatusChange(PGType pgType, PaymentDetailsDTO requestData) {
		try {
			if (pgType.equals(PGType.TOSS)) {
				TossPaymentDetailsDTO paymentData = objectMapper.convertValue(requestData,
					new TypeReference<>() {
					});

				String paymentKey = paymentData.getPaymentKey();
				PaymentGateway paymentGateway = paymentGatewayFactory.getGateway(pgType.name());
				PaymentQuery query = PaymentQuery.builder()
					.paymentKey(paymentKey)
					.build();
				paymentGateway.queryPayment(query)
					.doOnSubscribe(sub -> logger.info("결제 데이터 요청 시작됨: paymentKey={}", paymentKey))
					.doOnSuccess(response -> logger.info("결제 데이터 요청 성공: response={}", response))
					.doOnError(error -> logger.error("결제 데이터 요청 실패: paymentKey={}", paymentKey, error))
					.subscribe(response -> {
							logger.info("response: {}", response);
							TossPaymentConsistencyCheckDTO actualPayment = objectMapper.convertValue(
								response,
								TossPaymentConsistencyCheckDTO.class);

							TossPaymentConsistencyCheckDTO requestedPayment = objectMapper.convertValue(
								paymentData,
								TossPaymentConsistencyCheckDTO.class);

							if (!actualPayment.equals(requestedPayment)) {
								logger.error("결제 정보가 일치하지 않습니다. actualPayment={}, requestedPayment={}", actualPayment,
									requestedPayment);
								throw new PaymentInconsistencyException("결제 정보가 일치하지 않습니다.");
							}
						}
					);
			}
		} catch (PaymentInconsistencyException e) {
			return false;
		}
		return true;
	}

	@Override
	public void updatePaymentStatus(long paymentId, PaymentDetailsDTO paymentDetails) {
		Payment payment = paymentRepository.findById(paymentId).orElse(null);
		if (payment == null) {
			logger.error("결제 정보를 찾을 수 없습니다.");
			return;
		}

		String status = paymentDetails.getStatus();
		String transactionId = paymentDetails.getTransactionId();
		String paymentMethod = paymentDetails.getPaymentMethod();
		OffsetDateTime approvedAt = paymentDetails.getApprovedAt();

		logger.info("payment: {}, status: {}, transactionId: {}, paymentMethod: {}, approvedAt: {}", payment, status,
			transactionId, paymentMethod, approvedAt);

		switch (status) {
			case "DONE", "PAID" -> {
				try {
					payment.approve(transactionId, paymentMethod, approvedAt);
					paymentRepository.save(payment);
					logger.info("결제 상태 업데이트 완료: status={}", payment.getStatus());
				} catch (IllegalStateException e) {
					throw new PaymentCallbackException(e.getMessage());
				}
			}
			default -> throw new NoSuchStatusException(status);
		}
	}

	/**
	 * PENDING 상태의 결제 객체를 생성합니다.
	 * @param requestDTO 결제 요청에 대한 메타 데이터를 포함합니다.
	 * @return PENDING 상태의 Payment 객체를 반환합니다.
	 */
	private Payment createPendingPayment(PaymentRequestDTO requestDTO) {
		return Payment.builder()
			.pgType(PGType.valueOf(requestDTO.getPgType()))
			.orderId(requestDTO.getOrderId())
			.amount(requestDTO.getAmount())
			.status(PaymentStatus.PENDING)
			.extraData(requestDTO.getModuleSpecificData())
			.requestedAt(OffsetDateTime.now())
			.build();
	}
}
