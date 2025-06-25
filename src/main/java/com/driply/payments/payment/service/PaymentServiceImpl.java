package com.driply.payments.payment.service;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResultDTO;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.entity.PaymentStatus;
import com.driply.payments.payment.exception.NoSuchStatusException;
import com.driply.payments.payment.exception.PaymentCallbackException;
import com.driply.payments.payment.gateway.PaymentGateway;
import com.driply.payments.payment.gateway.PaymentGatewayFactory;
import com.driply.payments.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	private final PaymentGatewayFactory paymentGatewayFactory;
	private final PaymentRepository paymentRepository;
	private final ExecutorService paymentExecutor = Executors.newFixedThreadPool(20);

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
			log.error("비동기 결제 처리 중 예외 발생", e);

			return PaymentResultDTO.builder()
				.paymentId(null)
				.status(PaymentStatus.FAILED)
				.success(false)
				.message(e.getMessage())
				.build();
		}
	}

	@Override
	@Transactional
	public void updatePaymentStatus(Payment payment, PaymentDetailsDTO paymentDetails) {
		if (payment == null) {
			throw new NullPointerException("payment is null");
		}

		if (paymentDetails == null) {
			throw new NullPointerException("paymentDetails is null");
		}

		String status = paymentDetails.getStatus();
		String transactionId = paymentDetails.getTransactionId();
		String paymentMethod = paymentDetails.getPaymentMethod();
		OffsetDateTime approvedAt = paymentDetails.getApprovedAt();

		switch (status) {
			case "DONE", "PAID" -> {
				try {
					payment.approve(transactionId, paymentMethod, approvedAt);
					paymentRepository.save(payment);
					log.info("결제 상태 업데이트 완료: paymentId={}, status={}", payment.getPaymentId(), payment.getStatus());
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
