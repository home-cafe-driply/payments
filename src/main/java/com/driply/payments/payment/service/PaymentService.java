package com.driply.payments.payment.service;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResultDTO;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;

public interface PaymentService {
	Payment getPaymentById(Long paymentId);

	PaymentResultDTO processPaymentAsync(PaymentRequestDTO requestDTO);

	boolean validateStatusChange(PGType pgType, PaymentDetailsDTO requestData);

	void updatePaymentStatus(long paymentId, PaymentDetailsDTO status);
}
