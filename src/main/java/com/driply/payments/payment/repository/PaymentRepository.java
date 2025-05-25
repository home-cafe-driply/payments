package com.driply.payments.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.driply.payments.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
