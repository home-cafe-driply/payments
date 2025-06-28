package com.driply.payments.payment.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.TossPaymentRequestDTO;
import com.driply.payments.payment.repository.PaymentRepository;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class TossPaymentsGatewayTest {

	private MockWebServer mockWebServer;
	private TossPaymentsGateway tossPaymentsGateway;
	private PaymentRepository paymentRepository;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		WebClient webClient = WebClient.builder()
			.baseUrl(mockWebServer.url("/").toString())
			.build();

		tossPaymentsGateway = new TossPaymentsGateway(webClient);
		paymentRepository = mock(PaymentRepository.class);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	private PaymentRequestDTO createValidPaymentRequest() {
		return TossPaymentRequestDTO.builder()
			.paymentKey("test_payment_key")
			.amount(BigDecimal.valueOf(50000))
			.orderId("test_order_id")
			.build();
	}

	@Test
	@DisplayName("payment success test")
	void processPayment_success() throws Exception {
		// 1. MockWebServer가 반환할 가짜 JSON 응답 준비
		String response = """
			{
			  "mId": "tvivarepublica",
			  "lastTransactionKey": "txrd_a01jw7wstrkfphftfmm7e8r077t",
			  "paymentKey": "test_payment_key",
			  "orderId": "test_order_id",
			  "orderName": "토스 티셔츠 외 2건",
			  "taxExemptionAmount": 0,
			  "status": "DONE",
			  "requestedAt": "2025-05-27T12:32:37+09:00",
			  "approvedAt": "2025-05-27T12:32:56+09:00",
			  "useEscrow": false,
			  "cultureExpense": false,
			  "card": null,
			  "virtualAccount": null,
			  "transfer": null,
			  "mobilePhone": null,
			  "giftCertificate": null,
			  "cashReceipt": null,
			  "cashReceipts": null,
			  "discount": null,
			  "cancels": null,
			  "secret": "ps_yL0qZ4G1VOvWk9mWxbmM3oWb2MQY",
			  "type": "NORMAL",
			  "easyPay": {
			    "provider": "토스페이",
			    "amount": 50000,
			    "discountAmount": 0
			  },
			  "country": "KR",
			  "failure": null,
			  "isPartialCancelable": true,
			  "receipt": {
			    "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva202505271232379tT06&ref=PX"
			  },
			  "checkout": {
			    "url": "https://api.tosspayments.com/v1/payments/tviva202505271232379tT06/checkout"
			  },
			  "currency": "KRW",
			  "totalAmount": 50000,
			  "balanceAmount": 50000,
			  "suppliedAmount": 45455,
			  "vat": 4545,
			  "taxFreeAmount": 0,
			  "method": "간편결제",
			  "version": "2022-11-16",
			  "metadata": null
			}
			""";

		// 2. MockWebServer에 응답 enqueue (테스트 중 WebClient가 이 응답을 받게 됨)
		mockWebServer.enqueue(
			new MockResponse()
				.setResponseCode(200)
				.addHeader("Content-Type", "application/json; charset=utf-8")
				.setBody(response)
		);

		// 3. 결제 요청 DTO 생성
		PaymentRequestDTO requestDTO = createValidPaymentRequest();
		long paymentId = 1L;

		// 3. 메서드 실행 및 결과 검증
		StepVerifier.create(tossPaymentsGateway.processPayment(requestDTO, paymentId))
			.verifyComplete(); // 정상 완료 시그널

		// 4. MockWebServer에 실제로 요청이 들어왔는지 검증 (최대 5초 대기)
		RecordedRequest recordedRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
		assertNotNull(recordedRequest); // 요청이 들어왔는지 확인
		assertEquals("POST", recordedRequest.getMethod()); // HTTP 메서드 검증
		assertEquals("/v1/payments/confirm", recordedRequest.getPath()); // 요청 경로 검증
	}

	@Test
	@DisplayName("결제 실패")
	void processPayment_failure() throws Exception {
		// 1. MockWebServer가 반환할 400 응답 준비
		String response = """
			{
			  "version": "2022-11-16",
			  "traceId": "test_trace_id",
			  "error": {
				"code": "test_error_code",",
				"message": "test_error_message",
			  }
			}
			""";

		mockWebServer.enqueue(
			new MockResponse()
				.setResponseCode(400)
				.addHeader("Content-Type", "application/json; charset=utf-8")
				.setBody(response)
		);

		// 2. 테스트용 요청 DTO
		PaymentRequestDTO requestDTO = createValidPaymentRequest();
		long paymentId = 1L;

		// 3. 메서드 실행 및 결과 검증
		Mono<Void> result = tossPaymentsGateway.processPayment(requestDTO, paymentId);

		StepVerifier.create(result)
			.expectErrorMatches(throwable ->
				throwable.getMessage().contains("400") ||
					throwable.getMessage().contains("test_error_message")
			)
			.verify();

		// 4. 실제로 HTTP 요청이 들어갔는지 검증
		RecordedRequest recordedRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
		assertNotNull(recordedRequest);
		assertEquals("POST", recordedRequest.getMethod()); // HTTP 메서드 검증
		assertEquals("/v1/payments/confirm", recordedRequest.getPath()); // 요청 경로 검증
	}
}