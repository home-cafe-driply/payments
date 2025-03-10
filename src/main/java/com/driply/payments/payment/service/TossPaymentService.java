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

    /**
     * 토스페이먼츠사 api를 통해 결제 승인 요청을 보냅니다.
     * @param requestUri 위젯결제 혹은 일반결제인지 판단하기 위해 사용됩니다.
     * @param jsonBody paymentKey, orderId, amount 값을 포함해야 합니다.
     * @return 결제 승인 성공
     *         - 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     *         - 결제 한 건의 결제 상태, 결제 취소 기록, 매출 전표, 현금영수증 정보 등을 포함합니다.
     *         - 객체의 구성은 결제수단(카드, 가상계좌, 간편결제 등)에 따라 조금씩 달라집니다.
     *         결제 승인 실패
     *         - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
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
    @Override
    public Map<String, Object> customerAuthorization(String customerKey, String code) throws IOException {
        ObjectNode requestData = objectMapper.createObjectNode();
        requestData.put("grantType", "AuthorizationCode");
        requestData.put(CUSTOMER_KEY, customerKey);
        requestData.put("code", code);
        String url = "https://api.tosspayments.com/v1/brandpay/authorizations/access-token";
        return sendRequest(requestData, API_SECRET_KEY, url);
    }

    /**
     * 빌링키를 통해 토스페이먼츠 api 서버로 결제 승인 요청을 보냅니다. 요청 uri에 빌링키를 포함해야 합니다.
     * @param jsonBody billingKey, amount, customerKey, orderId, orderName를 포함해야 합니다.
     * @return 정기결제 성공
     *         - 카드 자동결제 승인에 성공하면 card 필드에 값이 있는 Payment 객체가 돌아옵니다.
     *         정기결제 실패
     *         - 카드 자동결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> confirmBilling(String jsonBody) throws IOException {
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        String billingKey = billingKeyMap.get(requestData.get(CUSTOMER_KEY).toString()).toString();
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY,
                "https://api.tosspayments.com/v1/billing/" + billingKey);
        return response;
    }

    /**
     * 토스페이먼츠 api 서버로 빌링키 발급 요청을 보냅니다.
     * @param jsonBody authKey, customerKey를 포함해야 합니다.
     * @return 빌링키 발급 성공
     *         - 등록된 카드 정보와 발급된 billingKey가 포함되어 있는 Billing 객체가 돌아옵니다.
     *         빌링키 발급 실패
     *         - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
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

    /**
     * 토스페이먼츠 api 서버로 브랜드페이 결제 승인 요청을 보냅니다.
     * paymentKey에 해당하는 결제를 인증하고 승인합니다. Basic 인증 방식을 사용합니다.
     * @param jsonBody paymentKey, amount, customerKey, orderId 를 포함해야 합니다.
     * @return 결제 승인에 성공했다면 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     *         결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> confirmBrandpay(String jsonBody) throws IOException {
        ObjectNode requestData = JsonUtil.parseStringToObjectNode(jsonBody);
        String url = "https://api.tosspayments.com/v1/brandpay/payments/confirm";
        Map<String, Object> response = sendRequest(requestData, API_SECRET_KEY, url);
        return response;
    }

    /**
     * 토스페이먼츠 api로 요청을 보내기 위해 사용됩니다.
     * @param requestData 요청을 보낼때 함께 보낼 데이터 입니다.
     * @param secretKey api 서버 인증에 사용되는 비밀키를 포함해야 합니다.
     * @param urlString 요청 엔드포인트
     * @return 응답 결과를 반환합니다.
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
     * 요청을 보내기 위한 HttpURLConnection 객체를 생성합니다.
     * @param secretKey 토스페이먼츠 api 요청시에 필요한 api key 값을 포함해야 합니다.
     * @param urlString 엔드포인트
     * @return 인증정보를 포함한 connection 객체를 반환합니다.
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
