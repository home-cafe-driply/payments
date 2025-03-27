package com.driply.payments.payment.service;

import com.driply.payments.payment.dto.BillingKeyRequestDTO;
import com.driply.payments.payment.dto.BillingRequestDTO;
import com.driply.payments.payment.dto.BrandpayRequestDTO;
import com.driply.payments.payment.dto.PaymentRequestDTO;

import java.io.IOException;
import java.util.Map;

public interface PaymentService {
    Map<String, Object> processPayment(String requestUri, PaymentRequestDTO requestDTO) throws IOException;
    Map<String, Object> customerAuthorization(String customerKey,  String code) throws IOException;
    Map<String, Object> confirmBilling(BillingRequestDTO requestDTO) throws IOException;
    Map<String, Object> issueBillingKey(BillingKeyRequestDTO requestDTO) throws IOException;
    Map<String, Object> confirmBrandpay(BrandpayRequestDTO requestDTO) throws IOException;
}
