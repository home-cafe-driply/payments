package com.driply.payments.payment.service;

import java.io.IOException;
import java.util.Map;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.entity.Payment;

public interface PaymentService {
    Map<String, Object> processPayment(PaymentRequestDTO requestDTO) throws IOException;
    Payment getPayment(Long paymentId);
}
