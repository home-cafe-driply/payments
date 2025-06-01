package com.driply.payments.payment.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.TossPaymentRequestDTO;
import com.driply.payments.payment.exception.TossApiException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class TossPaymentsGatewayTest {

	private MockWebServer mockWebServer;
	private TossPaymentsGateway tossPaymentsGateway;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		WebClient webClient = WebClient.builder()
			.baseUrl(mockWebServer.url("/").toString())
			.build();

		tossPaymentsGateway = new TossPaymentsGateway(webClient);
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
	@DisplayName("결제 성공")
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

		// 3. mockLogger 생성 및 TossPaymentsGateway에 리플렉션으로 주입
		Logger mockLogger = mock(Logger.class);
		ReflectionTestUtils.setField(tossPaymentsGateway, "logger", mockLogger);

		// 4. 결제 요청 DTO 생성
		PaymentRequestDTO requestDTO = createValidPaymentRequest();
		long paymentId = 1L;

		// 5. 결제 처리 메서드 호출 (비동기적으로 HTTP 요청 발생)
		tossPaymentsGateway.processPayment(requestDTO, paymentId);

		// 6. MockWebServer에 실제로 요청이 들어왔는지 검증 (최대 5초 대기)
		RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
		assertNotNull(recordedRequest); // 요청이 들어왔는지 확인
		assertEquals("POST", recordedRequest.getMethod()); // HTTP 메서드 검증
		assertEquals("/v1/payments/confirm", recordedRequest.getPath()); // 요청 경로 검증

		// 7. 로그 호출 순서 검증 (비동기 콜백 실행까지 최대 2초 기다림)
		InOrder inOrder = inOrder(mockLogger);
		inOrder.verify(mockLogger, timeout(2000)).info("결제 요청 시작됨: paymentId={}", 1L); // 먼저 호출되어야 함
		inOrder.verify(mockLogger, timeout(2000)).info("결제 요청 성공: paymentId={}", 1L); // 그 다음 호출되어야 함
	}

	@Test
	@DisplayName("결제 실패")
	void processPayment_failure() throws Exception {
		// 1. MockWebServer가 반환할 400 응답 준비
		String response = """
		{
		  "version": "2022-11-16",
		  "traceId": "{traceId}",
		  "error": {
			"code": "{CODE}",
			"message": "{MESSAGE}",
		  }
		}
		""";

		mockWebServer.enqueue(
			new MockResponse()
				.setResponseCode(400)
				.addHeader("Content-Type", "application/json; charset=utf-8")
				.setBody(response)
		);

		// 2. 동기화 도구 준비
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// 3. logger를 spy로 교체하여 error 호출 시 latch와 예외 저장
		Logger spyLogger = spy(Logger.class);
		ReflectionTestUtils.setField(tossPaymentsGateway, "logger", spyLogger);

		doAnswer(invocation -> {
			// error(String, Object, Throwable) 시그니처에 맞게 처리
			Throwable error = invocation.getArgument(2, Throwable.class);
			errorRef.set(error);
			latch.countDown();
			return null;
		}).when(spyLogger).error(anyString(), anyLong(), any(Throwable.class));

		// 4. 결제 요청 DTO 생성
		PaymentRequestDTO requestDTO = createValidPaymentRequest();
		long paymentId = 1L;

		// 5. 결제 처리 메서드 호출
		tossPaymentsGateway.processPayment(requestDTO, paymentId);

		// 6. 비동기 콜백이 실행될 때까지 대기
		assertTrue(latch.await(5, TimeUnit.SECONDS), "비동기 콜백이 호출되지 않았습니다.");

		// 7. 예외 객체 검증
		Throwable error = errorRef.get();
		assertNotNull(error, "에러가 null이면 안 됩니다.");
		assertInstanceOf(TossApiException.class, error, "TossApiException이어야 합니다.");
		assertTrue(error.getMessage().contains("토스 API 오류"));
		assertTrue(((TossApiException) error).getResponseBody().contains("\"code\": \"{CODE}\""));
	}
}