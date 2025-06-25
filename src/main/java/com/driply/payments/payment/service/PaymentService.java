package com.driply.payments.payment.service;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResultDTO;
import com.driply.payments.payment.entity.Payment;

public interface PaymentService {
	Payment getPaymentById(Long paymentId);

	PaymentResultDTO processPaymentAsync(PaymentRequestDTO requestDTO);

	void updatePaymentStatus(Payment payment, PaymentDetailsDTO paymentDetails);
}
