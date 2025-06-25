package com.driply.payments.payment.gateway;

import java.util.Map;

import com.driply.payments.payment.dto.PaymentDetailsDTO;
import com.driply.payments.payment.entity.Payment;

public interface PaymentCallbackGateway {
	String getPgName();

	PaymentDetailsDTO parsePaymentDetails(Map<String, Object> callbackData);

	Payment findPayment(PaymentDetailsDTO paymentDetails);

	void validateStatusChange(PaymentDetailsDTO paymentDetails);
}
