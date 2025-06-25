package com.driply.payments.payment.gateway;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.PaymentQuery;
import com.driply.payments.payment.dto.TossPaymentConsistencyCheckDTO;
import com.driply.payments.payment.dto.TossPaymentDetailsDTO;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.exception.PaymentCallbackException;
import com.driply.payments.payment.exception.PaymentInconsistencyException;
import com.driply.payments.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentCallbackGateway implements PaymentCallbackGateway {
	private final PaymentGatewayFactory paymentGatewayFactory;
	private final PaymentRepository paymentRepository;
	private final ObjectMapper objectMapper;

	@Override
	public String getPgName() {
		return PGType.TOSS.name();
	}

	/**
	 * 콜백 데이터에서 Toss 결제 상세 정보를 파싱합니다.
	 * <p>
	 * "data" 필드를 TossPaymentDetailsDTO로 변환합니다.
	 *
	 * @param callbackData 콜백 데이터 맵
	 * @return Toss 결제 상세 정보 DTO
	 * @throws PaymentCallbackException "data" 필드가 없을 경우
	 */
	@Override
	public PaymentDetailsDTO parsePaymentDetails(Map<String, Object> callbackData) {
		Object data = callbackData.get("data");
		if (data == null) {
			throw new PaymentCallbackException("Toss 콜백 데이터가 없습니다.");
		}
		return objectMapper.convertValue(data, TossPaymentDetailsDTO.class);
	}

	/**
	 * 결제 상세 정보로 결제 엔티티를 조회합니다.
	 *
	 * @param paymentDetails Toss 결제 상세 정보 DTO
	 * @return 조회된 결제 엔티티, 없으면 null
	 * @throws PaymentCallbackException Toss 결제 상세 정보가 아닐 경우
	 */
	@Override
	public Payment findPayment(PaymentDetailsDTO paymentDetails) {
		if (!(paymentDetails instanceof TossPaymentDetailsDTO tossDetails)) {
			throw new PaymentCallbackException("Toss 결제 상세 정보가 아닙니다.");
		}

		return paymentRepository.findByPaymentKey(tossDetails.getPaymentKey()).orElse(null);
	}

	/**
	 * 결제 상태 변경의 일관성을 검증합니다.
	 * <p>
	 * PG사로부터 실제 결제 정보를 조회하여 콜백 데이터와 비교합니다.
	 *
	 * @param paymentDetails Toss 결제 상세 정보 DTO
	 * @throws PaymentCallbackException Toss 결제 상세 정보가 아닐 경우
	 * @throws PaymentInconsistencyException 결제 정보가 일치하지 않을 경우
	 */
	@Override
	public void validateStatusChange(PaymentDetailsDTO paymentDetails) {
		if (!(paymentDetails instanceof TossPaymentDetailsDTO tossDetails)) {
			throw new PaymentCallbackException("Toss 결제 상세 정보가 아닙니다.");
		}

		String paymentKey = tossDetails.getPaymentKey();
		PaymentGateway paymentGateway = paymentGatewayFactory.getGateway(getPgName());
		PaymentQuery query = PaymentQuery.builder()
			.paymentKey(paymentKey)
			.build();
		TossPaymentDetailsDTO paymentDetailsDTO = paymentGateway.queryPayment(query).block();
		log.info("response: {}", paymentDetailsDTO);
		TossPaymentConsistencyCheckDTO actualPayment = objectMapper.convertValue(
			paymentDetailsDTO,
			TossPaymentConsistencyCheckDTO.class);

		TossPaymentConsistencyCheckDTO requestedPayment = objectMapper.convertValue(
			tossDetails,
			TossPaymentConsistencyCheckDTO.class);

		if (!actualPayment.equals(requestedPayment)) {
			throw new PaymentInconsistencyException(
				"결제 정보가 일치하지 않습니다. actualPayment=%s, requestedPayment=%s".formatted(actualPayment,
					requestedPayment));
		}
	}
}
