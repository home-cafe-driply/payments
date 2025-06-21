package com.driply.payments.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.driply.payments.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	@Query(value = "SELECT * FROM payments WHERE extra_data->>'paymentKey' = :paymentKey", nativeQuery = true)
	Payment findByPaymentKey(@Param("paymentKey") String paymentKey);
}
