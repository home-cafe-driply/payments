package com.driply.payments.payment.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.driply.payments.common.JsonUtil;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.PaymentError;
import com.driply.payments.payment.entity.PaymentStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TossPaymentsGateway implements PaymentGateway {
    private static final ObjectMapper objectMapper = JsonUtil.objectMapper;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WebClient tossWebClient;

    @Value("${toss.payments.test.widget-secret-key}")
    private String WIDGET_SECRET_KEY;
    @Value("${toss.payments.api-secret-key}")
    private String API_SECRET_KEY;

    @Override
    public Map<String, Object> processPayment(PaymentRequestDTO requestDTO, long paymentId) {
        Map<String, Object> response = new HashMap<>();
        try {
            sendPaymentRequest(requestDTO);
        } catch (Exception e) {
        }
        return response;
    }

    @Override
    public PaymentStatus checkStatus(String transactionId) {
        return null;
    }

    @Override
    public PaymentError getLastError() {
        return null;
    }

    @Override
    public String getPGType() {
        return PGType.TOSS.name();
    }

    @Override
    public boolean refundPayment(String transactionId) {
        return false;
    }

    /**
     * Toss payments로 결제 요청을 보내기 위해
     * 비동기적으로 요청을 처리하며, 웹훅 기능을 사용하여 결제를 처리하기 때문에 별도의 응답을 수신하거나 응답을 반환하지 않습니다.
     * @param requestDTO 결제 요청에 필요한 데이터. paymentKey, orderId, amount, requestUri 값을 포함 합니다.
     */
    private void sendPaymentRequest(PaymentRequestDTO requestDTO) throws IOException {
        Map<String, Object> moduleSpecificData = requestDTO.getModuleSpecificData();
        String requestUri = moduleSpecificData.get("requestUri").toString();
        String paymentKey = moduleSpecificData.get("paymentKey").toString();
        String orderId = requestDTO.getOrderId();
        BigDecimal amount = requestDTO.getAmount();

        ObjectNode requestData = JsonUtil.parseObjectNode(
                Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderId,
                        "amount", amount
                )
        );
        String secretKey = requestUri.contains("/confirm/payment") ? API_SECRET_KEY : WIDGET_SECRET_KEY;
        String url = "/v1/payments/confirm";

        CompletableFuture.runAsync(() -> sendRequestAsync(requestData, secretKey, url));
    }

    /**
     * 지정한 Url로 비동기 요청을 보냅니다.
     * @param requestData 요청 본문
     * @param secretKey api 서버 인증에 사용되는 비밀키
     * @param urlString 요청 엔드포인트
     */
    private void sendRequestAsync(ObjectNode requestData, String secretKey, String urlString) {
        tossWebClient.post()
            .uri(urlString)
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestData)
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(
                unused -> {},
                error -> {
                    throw new RuntimeException("비동기 API 호출 실패", error);
                }
            );
    }

    /**
     * 토스페이먼츠로 요청을 보내기 위해 사용됩니다. HttpURLConnection을 생성하여 요청을 동기적으로 처리합니다.
     * @param requestData 요청을 보낼때 함께 보낼 데이터입니다.
     * @param secretKey api 서버 인증에 사용되는 비밀키를 포함해야 합니다.
     * @param urlString 요청 엔드포인트
     * @return 토스페이먼츠의 api 응답 결과를 반환합니다.
     * @throws IOException
     */
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

    /**
     * 요청을 보내기 위한 커넥션을 생성합니다.
     * @param secretKey 토스페이먼츠 api 요청시에 필요한 api key 값을 포함해야 합니다.
     * @param urlString 엔드포인트
     * @return 인증정보를 포함한 HttpURLConnection 객체를 반환합니다.
     * @throws IOException
     */
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
