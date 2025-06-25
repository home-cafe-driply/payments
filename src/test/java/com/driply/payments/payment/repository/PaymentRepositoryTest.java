package com.driply.payments.payment.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.driply.payments.payment.dto.PGType;
import com.driply.payments.payment.dto.PaymentStatus;
import com.driply.payments.payment.entity.Payment;

@SpringBootTest
class PaymentRepositoryTest {

	@Autowired
	private PaymentRepository paymentRepository;

	private Payment payment;

	@BeforeEach
	void setup() {
		payment = Payment.builder()
			.pgType(PGType.valueOf(PGType.TOSS.name()))
			.orderId("test_order_id")
			.amount(BigDecimal.valueOf(50000))
			.status(PaymentStatus.PENDING)
			.extraData(Map.of("paymentKey", "test_payment_key"))
			.requestedAt(OffsetDateTime.now())
			.build();

		paymentRepository.save(payment);
	}

	@Test
	@DisplayName("find by paymentKey")
	void findByPaymentKey() {
		Payment testPayment = paymentRepository.findByPaymentKey("test_payment_key").orElse(null);

		assertNotNull(testPayment);
		assertEquals("test_payment_key", testPayment.getExtraData().get("paymentKey"));
	}
}