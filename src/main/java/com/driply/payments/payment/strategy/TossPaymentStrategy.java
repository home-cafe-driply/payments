package com.driply.payments.payment.strategy;

import com.driply.payments.common.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

@Component
public class TossPaymentStrategy implements PaymentStrategy {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, Object> billingKeyMap = new HashMap<>();
    private static final ObjectMapper objectMapper = JsonUtil.objectMapper;
    @Value("${toss.payments.test.widget-secret-key}")
    private String WIDGET_SECRET_KEY;
    @Value("${toss.payments.api-secret-key}")
    private String API_SECRET_KEY;

    /**
     * Toss payments로 결제 요청을 보냅니다.
     * @param requestData 요청 데이터. paymentKey, orderId, amount 값을 포함해야 합니다.
     * @param requestUri 요청 uri 입니다.
     *                   일반 결제 uri의 경우 API_SECRET_KEY를 사용하고, 위젯 결제 uri의 경우 WIDGET_SECRET_KEY를 사용합니다.
     * @return 결제 승인 성공
     *         - 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     *         - 결제 한 건의 결제 상태, 결제 취소 기록, 매출 전표, 현금영수증 정보 등을 포함합니다.
     *         - 객체의 구성은 결제수단(카드, 가상계좌, 간편결제 등)에 따라 조금씩 달라집니다.
     *         결제 승인 실패
     *         - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    public Map<String, Object> sendPaymentRequest(String requestUri, ObjectNode requestData) throws IOException {
        logger.info("request data: {}", requestData);
        String secretKey = requestUri.contains("/confirm/payment") ? API_SECRET_KEY : WIDGET_SECRET_KEY;
        String url = "https://api.tosspayments.com/v1/payments/confirm";
        return sendRequest(requestData, secretKey, url);
    }

    /**
     * 토스페이먼츠사 api를 통해 사용자 access token의 발급 요청을 보냅니다.
     * customerKey, grantType, code 필드를 포함해야 합니다.
     * 발급을 위한 api이기 때문에 grantType은 AuthorizationCode로 고정입니다.
     * grantType이 AuthorizationCode일 때 code 필드는 필수 입니다.
     * @param customerKey 상점에서 만든 고객의 고유 ID입니다.
     * @param code Access Token 발급에 필요한 Authorization Code(임시 인증 코드)입니다.
     * @return 응답으로 Access Token, Access Token의 유효기간, Refresh Token이 돌아옵니다.
     * @throws IOException
     */
    public Map<String, Object> sendAuthorizationRequest(String customerKey, String code) throws IOException {
        ObjectNode requestData = objectMapper.createObjectNode();
        requestData.put("grantType", "AuthorizationCode");
        requestData.put("customerKey", customerKey);
        requestData.put("code", code);
        String url = "https://api.tosspayments.com/v1/brandpay/authorizations/access-token";
        return sendRequest(requestData, API_SECRET_KEY, url);
    }

    /**
     * 빌링키를 통해 토스페이먼츠 api 서버로 결제 승인 요청을 보냅니다. 요청 uri에 빌링키를 포함해야 합니다.
     * @param requestData billingKey, amount, customerKey, orderId, orderName를 포함해야 합니다.
     * @return 정기결제 성공
     *         - 카드 자동결제 승인에 성공하면 card 필드에 값이 있는 Payment 객체가 돌아옵니다.
     *         정기결제 실패
     *         - 카드 자동결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    public Map<String, Object> sendBillingConfirmRequest(ObjectNode requestData) throws IOException {
        String billingKey = billingKeyMap.get(requestData.get("customerKey").toString()).toString();
        String url = "https://api.tosspayments.com/v1/billing/";
        return sendRequest(requestData, API_SECRET_KEY,  url + billingKey);
    }

    /**
     * 빌링키를 통해 토스페이먼츠 api 서버로 결제 승인 요청을 보냅니다. 요청 uri에 빌링키를 포함해야 합니다.
     * @param requestData billingKey, amount, customerKey, orderId, orderName를 포함해야 합니다.
     * @return 정기결제 성공
     *         - 카드 자동결제 승인에 성공하면 card 필드에 값이 있는 Payment 객체가 돌아옵니다.
     *         정기결제 실패
     *         - 카드 자동결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    public Map<String, Object> sendBillingKeyRequest(ObjectNode requestData) throws IOException {
        String url ="https://api.tosspayments.com/v1/billing/authorizations/issue";
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY, url);
        if (!response.containsKey("error")) {
            billingKeyMap.put(requestData.get("customerKey").toString(), response.get("billingKey"));
        }
        return response;
    }

    /**
     * 브랜드페이
     * @param requestData
     * @return
     * @throws IOException
     */
    public Map<String, Object> sendBrandpayRequest(ObjectNode requestData) throws IOException {
        String url ="https://api.tosspayments.com/v1/billing/brandpay";
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY, url);
        return response;
    }

    /**
     * 토스페이먼츠로 요청을 보내기 위해 사용됩니다.
     * @param requestData 요청을 보낼때 함께 보낼 데이터입니다.
     * @param secretKey api 서버 인증에 사용되는 비밀키를 포함해야 합니다.
     * @param urlString 요청 엔드포인트
     * @return 토스페이먼츠의 api 응답 결과를 반환합니다.
     * @throws IOException
     */
    public Map<String, Object> sendRequest(ObjectNode requestData, String secretKey, String urlString) throws IOException {
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
    public HttpURLConnection createConnection(String secretKey, String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8)));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }
}
