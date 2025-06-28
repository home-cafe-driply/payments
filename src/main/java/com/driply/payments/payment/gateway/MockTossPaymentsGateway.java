package com.driply.payments.payment.gateway;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.driply.payments.payment.dto.PGType;
import com.driply.payments.payment.dto.PaymentError;
import com.driply.payments.payment.dto.PaymentQuery;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentStatus;
import com.driply.payments.payment.dto.TossPaymentDetailsDTO;
import com.driply.payments.payment.exception.TossApiException;
import com.driply.payments.payment.exception.TossConnectionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Profile("test")
@Slf4j
@Component
@RequiredArgsConstructor
public class MockTossPaymentsGateway implements PaymentGateway {
	private static final String PAYMENT_CONFIRM_URI = "/v1/payments/confirm";
	private static final String AUTH_HEADER = "Authorization";
	private static final String AUTH_PREFIX = "Basic ";

	@Value("${toss.payments.test.widget-secret-key}")
	private String WIDGET_SECRET_KEY;
	@Value("${toss.payments.api-secret-key}")
	private String API_SECRET_KEY;

	/**
	 * 결제 요청을 처리합니다.
	 * <p>
	 * 결제 요청 데이터의 유효성을 검증한 뒤, 토스 결제 API로 비동기 결제 요청을 전송합니다.<br>
	 * 요청의 시작, 성공, 실패 시점에 각각 로그를 기록합니다.<br>
	 * 결제 요청 중 예외가 발생하면 에러 로그를 남기고, dropped exception 방지를 위해
	 * subscribe의 onError 콜백을 명시적으로 작성합니다.<br>
	 * <b>실제 결제 결과 및 후속 처리는 토스에서 호출하는 콜백(웹훅) 엔드포인트를 통해 별도로 처리됩니다.</b>
	 *
	 * @param requestDTO 결제 요청 데이터 객체
	 * @param paymentId 결제 트랜잭션 식별자
	 * @throws IllegalArgumentException 결제 요청 데이터가 유효하지 않은 경우
	 */
	@Override
	public Mono<Void> processPayment(PaymentRequestDTO requestDTO, long paymentId) {
		validatePaymentRequest(requestDTO);

		return sendPaymentRequest(requestDTO)
			.doOnSubscribe(sub -> log.info("결제 요청 시작됨: paymentId={}", paymentId))
			.doOnSuccess(response ->
				log.info("결제 요청 성공: paymentId={}", paymentId))
			.doOnError(error ->
				log.error("결제 요청 실패: paymentId={}", paymentId, error))
			.then();
	}

	/**
	 * 결제 키를 이용하여 토스 결제 상세 정보를 조회합니다.
	 *
	 * @param query 결제 조회에 필요한 정보를 담고 있는 PaymentQuery 객체
	 * @return 결제 상세 정보를 포함하는 Mono<TossPaymentDetailsDTO>
	 */
	@Override
	public Mono<TossPaymentDetailsDTO> queryPayment(PaymentQuery query) {
		return Mono.just(TossPaymentDetailsDTO.builder().build());
	}

	@Override
	public PaymentStatus checkStatus(String transactionId) {
		return null;
	}

	@Override
	public PaymentError getLastError() {
		return null;
	}

	@Override
	public String getPGType() {
		return PGType.TOSS.name();
	}

	@Override
	public boolean refundPayment(String transactionId) {
		return false;
	}

	/**
	 * 토스페이먼츠 결제 승인 요청을 비동기적으로 전송합니다.
	 * <p>
	 * WebClient를 사용하여 토스 결제 승인 API에 POST 요청을 보내고,
	 * 10초 내 응답이 없으면 타임아웃 예외를 발생시킵니다.
	 * API 오류 및 네트워크 오류는 {@link #wrapException(Throwable)}을 통해
	 * 커스텀 예외로 변환됩니다.
	 *
	 * @param requestDTO 결제 요청 데이터 DTO
	 * @return 결제 승인 응답을 포함하는 Mono. (응답 본문은 String)
	 *         에러 발생 시 TossApiException 또는 TossConnectionException이 발생합니다.
	 */
	private Mono<String> sendPaymentRequest(PaymentRequestDTO requestDTO) {
		return Mono.just("{\"status\":\"SUCCESS\",\"paymentKey\":\"MOCK_KEY\"}");
	}

	/**
	 * WebClient 호출 중 발생한 예외를 커스텀 예외로 변환합니다.
	 * <p>
	 * HTTP 오류 응답(WebClientResponseException)은 TossApiException으로,
	 * 그 외 네트워크/시스템 오류는 TossConnectionException으로 변환합니다.
	 *
	 * @param e WebClient 호출 중 발생한 원본 예외
	 * @return TossApiException 또는 TossConnectionException 인스턴스
	 */
	private Throwable wrapException(Throwable e) {
		if (e instanceof WebClientResponseException ex) {
			return new TossApiException(
				"토스 API 오류: " + ex.getStatusCode(),
				ex.getResponseBodyAsString()
			);
		}
		return new TossConnectionException("토스 연결 실패: " + e.getMessage(), e);
	}

	/**
	 * 결제 API 인증 헤더를 생성합니다.
	 * <p>
	 * secretKey를 Base64 인코딩하여 Basic 인증 헤더 포맷으로 반환합니다.
	 * @param secretKey 결제 API 시크릿 키
	 * @return Base64 인코딩된 인증 헤더 값
	 */
	private String createAuthHeader(String secretKey) {
		return AUTH_PREFIX + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 결제 요청 데이터의 유효성을 검증합니다.
	 * <p>
	 * 다음 조건을 검사합니다:
	 * <ul>
	 *   <li>요청 객체가 null이 아닌지 확인</li>
	 *   <li>주문 ID(orderId)가 null이 아니고, 공백이 아닌지 확인</li>
	 *   <li>결제 금액(amount)이 null이 아니고, 0보다 큰지 확인</li>
	 * </ul>
	 * 유효하지 않은 경우 {@link IllegalArgumentException} 예외를 발생시킵니다.
	 *
	 * @param requestDTO 검증할 결제 요청 데이터 객체
	 * @throws IllegalArgumentException 요청 데이터가 null이거나, 주문 ID 또는 결제 금액이 유효하지 않은 경우
	 */
	private void validatePaymentRequest(PaymentRequestDTO requestDTO) {
		if (requestDTO == null) {
			throw new IllegalArgumentException("결제 요청 데이터가 null입니다.");
		}
		if (requestDTO.getOrderId() == null || requestDTO.getOrderId().trim().isEmpty()) {
			throw new IllegalArgumentException("주문 ID가 필요합니다.");
		}
		if (requestDTO.getAmount() == null || requestDTO.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("결제 금액이 유효하지 않습니다.");
		}
	}
}