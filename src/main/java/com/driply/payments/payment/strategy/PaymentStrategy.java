package com.driply.payments.payment.strategy;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

public interface PaymentStrategy {
    Map<String, Object> sendPaymentRequest(String requestUri, ObjectNode requestData) throws IOException;
    Map<String, Object> sendAuthorizationRequest(String customerKey, String code) throws IOException;
    Map<String, Object> sendBillingConfirmRequest(ObjectNode requestData) throws IOException;
    Map<String, Object> sendBillingKeyRequest(ObjectNode requestData) throws IOException;
    Map<String, Object> sendBrandpayRequest(ObjectNode requestData) throws IOException;
    Map<String, Object> sendRequest(ObjectNode requestData, String secretKey, String urlString) throws IOException;
    HttpURLConnection createConnection(String secretKey, String urlString) throws IOException;
}
