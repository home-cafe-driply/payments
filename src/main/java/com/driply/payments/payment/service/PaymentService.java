package com.driply.payments.payment.service;

import java.io.IOException;
import java.util.Map;

public interface PaymentService {
    Map<String, Object> processPayment(String requestUri, String jsonBody) throws IOException;
    Map<String, Object> customerAuthorization(String customerKey,  String code) throws IOException;
    Map<String, Object> confirmBilling(String jsonBody) throws IOException;
    Map<String, Object> issueBillingKey(String jsonBody) throws IOException;
    Map<String, Object> confirmBrandpay(String jsonBody) throws IOException;
}
