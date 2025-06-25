package com.driply.payments.payment.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.driply.payments.payment.dto.PGType;
import com.driply.payments.payment.dto.TossPaymentConsistencyCheckDTO;
import com.driply.payments.payment.dto.TossPaymentDetailsDTO;
import com.driply.payments.payment.exception.PaymentInconsistencyException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@SpringBootTest
class TossPaymentCallbackGatewayTest {

	@Mock
	private PaymentGatewayFactory paymentGatewayFactory;

	@Mock
	private PaymentGateway paymentGateway;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private TossPaymentCallbackGateway tossPaymentCallbackGateway;

	@BeforeEach
	void setUp() {
		try (AutoCloseable mocks = MockitoAnnotations.openMocks(this)) {
			tossPaymentCallbackGateway = new TossPaymentCallbackGateway(paymentGatewayFactory, null, objectMapper);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("결제 정보 일치 시 예외 발생하지 않음")
	void validateStatusChange_success() {
		String paymentKey = "test-payment-key";
		String orderId = "test-order-id";
		String orderName = "test-order-name";
		OffsetDateTime requestedAt = OffsetDateTime.now();
		OffsetDateTime approvedAt = OffsetDateTime.now();
		String secret = "test-secret";
		String status = "DONE";
		BigDecimal totalAmount = BigDecimal.valueOf(10000);

		TossPaymentDetailsDTO tossDetails = mock(TossPaymentDetailsDTO.class);
		when(tossDetails.getPaymentKey()).thenReturn(paymentKey);

		TossPaymentDetailsDTO queriedDetails = mock(TossPaymentDetailsDTO.class);

		TossPaymentConsistencyCheckDTO actual = TossPaymentConsistencyCheckDTO.builder()
			.paymentKey(paymentKey)
			.orderId(orderId)
			.orderName(orderName)
			.requestedAt(requestedAt)
			.approvedAt(approvedAt)
			.secret(secret)
			.status(status)
			.totalAmount(totalAmount)
			.build();
		TossPaymentConsistencyCheckDTO requested = TossPaymentConsistencyCheckDTO.builder()
			.paymentKey(paymentKey)
			.orderId(orderId)
			.orderName(orderName)
			.requestedAt(requestedAt)
			.approvedAt(approvedAt)
			.secret(secret)
			.status(status)
			.totalAmount(totalAmount)
			.build();

		when(objectMapper.convertValue(queriedDetails, TossPaymentConsistencyCheckDTO.class)).thenReturn(actual);
		when(objectMapper.convertValue(tossDetails, TossPaymentConsistencyCheckDTO.class)).thenReturn(requested);
		assertEquals(actual, requested);

		when(paymentGatewayFactory.getGateway(PGType.TOSS.name())).thenReturn(paymentGateway);
		when(paymentGateway.queryPayment(any())).thenReturn(Mono.just(queriedDetails));

		assertDoesNotThrow(() -> tossPaymentCallbackGateway.validateStatusChange(tossDetails));
	}

	@Test
	@DisplayName("결제 정보가 일치하지 않을 경우 PaymentInconsistencyException 발생")
	void validateStatusChange_inconsistency() {
		String paymentKey = "test-payment-key";
		String orderId = "test-order-id";
		String orderName = "test-order-name";
		OffsetDateTime requestedAt = OffsetDateTime.now();
		OffsetDateTime approvedAt = OffsetDateTime.now();
		String secret = "test-secret";
		String status = "DONE";
		BigDecimal totalAmount = BigDecimal.valueOf(10000);

		TossPaymentDetailsDTO tossDetails = mock(TossPaymentDetailsDTO.class);
		when(tossDetails.getPaymentKey()).thenReturn(paymentKey);

		TossPaymentDetailsDTO queriedDetails = mock(TossPaymentDetailsDTO.class);

		TossPaymentConsistencyCheckDTO actual = TossPaymentConsistencyCheckDTO.builder()
			.paymentKey(paymentKey)
			.orderId(orderId)
			.orderName(orderName)
			.requestedAt(requestedAt)
			.approvedAt(approvedAt)
			.secret(secret)
			.status(status)
			.totalAmount(totalAmount)
			.build();
		TossPaymentConsistencyCheckDTO requested = TossPaymentConsistencyCheckDTO.builder()
			.paymentKey(paymentKey)
			.orderId(orderId)
			.orderName(orderName)
			.requestedAt(requestedAt)
			.approvedAt(approvedAt)
			.secret(secret)
			.status(status)
			.totalAmount(BigDecimal.valueOf(1000))
			.build();

		when(objectMapper.convertValue(queriedDetails, TossPaymentConsistencyCheckDTO.class)).thenReturn(actual);
		when(objectMapper.convertValue(tossDetails, TossPaymentConsistencyCheckDTO.class)).thenReturn(requested);
		assertNotEquals(actual, requested);

		when(paymentGatewayFactory.getGateway(PGType.TOSS.name())).thenReturn(paymentGateway);
		when(paymentGateway.queryPayment(any())).thenReturn(Mono.just(queriedDetails));

		assertThrows(PaymentInconsistencyException.class,
			() -> tossPaymentCallbackGateway.validateStatusChange(tossDetails));
	}
}