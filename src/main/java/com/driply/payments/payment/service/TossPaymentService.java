package com.driply.payments.payment.service;

import com.driply.payments.common.JsonUtil;
import com.driply.payments.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:application-secret.yml")
public class TossPaymentService implements PaymentService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> billingKeyMap = new HashMap<>();
    private final PaymentRepository paymentRepository;
    private static final String CUSTOMER_KEY = "customerKey";
    @Value("${toss.payments.test.widget-secret-key}")
    private String WIDGET_SECRET_KEY;
    @Value("${toss.payments.api-secret-key}")
    private String API_SECRET_KEY;

    @Override
    public Map<String, Object> processPayment(String requestUri, String jsonBody) throws IOException {
        logger.info("Processing request URI: {}", requestUri);
        logger.info("Processing request JSON body: {}", jsonBody);
        String secretKey = requestUri.contains("/confirm/payment") ? API_SECRET_KEY : WIDGET_SECRET_KEY;
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        logger.info("Request data: {}", requestData);
        Map<String, Object> response = sendRequest(requestData, secretKey,
                "https://api.tosspayments.com/v1/payments/confirm");

        logger.info("Response from Toss Payment Service: {}", response);
        // TODO: payment 응답 결과 DB에 저장
        return response;
    }

    @Override
    public Map<String, Object> customerAuthorization(String customerKey, String code) throws IOException {
        ObjectNode requestData = objectMapper.createObjectNode();
        requestData.put("grantType", "AuthorizationCode");
        requestData.put(CUSTOMER_KEY, customerKey);
        requestData.put("code", code);
        String url = "https://api.tosspayments.com/v1/brandpay/authorizations/access-token";
        return sendRequest(requestData, API_SECRET_KEY, url);
    }

    @Override
    public Map<String, Object> confirmBilling(String jsonBody) throws IOException {
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        String billingKey = billingKeyMap.get(requestData.get(CUSTOMER_KEY).toString()).toString();
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY,
                "https://api.tosspayments.com/v1/billing/" + billingKey);
        return response;
    }

    @Override
    public Map<String, Object> issueBillingKey(String jsonBody) throws IOException {
        logger.info("Issue request JSON body: {}", jsonBody);
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY,
                "https://api.tosspayments.com/v1/billing/authorizations/issue");
        logger.info("Response from Toss Payment Service: {}", response);
        if (!response.containsKey("error")) {
            billingKeyMap.put(requestData.get(CUSTOMER_KEY).toString(), response.get("billingKey"));
        }
        return response;
    }

    @Override
    public Map<String, Object> confirmBrandpay(String jsonBody) throws IOException {
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        String url = "https://api.tosspayments.com/v1/brandpay/payments/confirm";
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY, url);
        return response;
    }

    private Map<String, Object> sendRequest(ObjectNode requestData, String secretKey, String urlString) throws IOException {
        HttpURLConnection connection = createConnection(secretKey, urlString);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestData.toString().getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
             Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            return objectMapper.readValue(reader, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Error reading response", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error reading response");
            return errorResponse;
        }
    }

    private HttpURLConnection createConnection(String secretKey, String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }
}
