package com.driply.payments.payment.service;

import com.driply.payments.common.JsonUtil;
import com.driply.payments.payment.dto.BillingKeyRequestDTO;
import com.driply.payments.payment.dto.BillingRequestDTO;
import com.driply.payments.payment.dto.BrandpayRequestDTO;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.repository.PaymentRepository;
import com.driply.payments.payment.strategy.PaymentStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:application-secret.yml")
public class TossPaymentService implements PaymentService {
    private final PaymentStrategy tossPaymentStrategy;
    private final PaymentRepository paymentRepository;

    /**
     * 토스페이먼츠사 api를 통해 결제 승인 요청을 보내기 위해 데이터 전처리하여 sendPaymentRequest() 메소드를 호출합니다.
     * @param requestUri 요청 uri. 위젯결제 혹은 일반결제인지 판단하기 위해 사용됩니다.
     * @param requestDTO paymentKey, orderId, amount 값을 포함해야 합니다.
     * @return 결제 승인 성공
     *         - 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     *         - 결제 한 건의 결제 상태, 결제 취소 기록, 매출 전표, 현금영수증 정보 등을 포함합니다.
     *         - 객체의 구성은 결제수단(카드, 가상계좌, 간편결제 등)에 따라 조금씩 달라집니다.
     *         결제 승인 실패
     *         - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> processPayment(String requestUri, PaymentRequestDTO requestDTO) throws IOException {
        ObjectNode requestData = JsonUtil.parseObjectNode(requestDTO);
        Map<String, Object> response = tossPaymentStrategy.sendPaymentRequest(requestUri, requestData);
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
        Map<String, Object> response = tossPaymentStrategy.sendAuthorizationRequest(customerKey, code);
        return response;
    }

    /**
     * 빌링키를 통해 토스페이먼츠 api 서버로 결제 승인 요청을 보냅니다. 요청 uri에 빌링키를 포함해야 합니다.
     * @param requestDTO billingKey, amount, customerKey, orderId, orderName를 포함해야 합니다.
     * @return 정기결제 성공
     *         - 카드 자동결제 승인에 성공하면 card 필드에 값이 있는 Payment 객체가 돌아옵니다.
     *         정기결제 실패
     *         - 카드 자동결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> confirmBilling(BillingRequestDTO requestDTO) throws IOException {
        ObjectNode requestData = JsonUtil.parseObjectNode(requestDTO);
        Map<String, Object> response = tossPaymentStrategy.sendBillingConfirmRequest(requestData);
        return response;
    }

    /**
     * 토스페이먼츠 api 서버로 빌링키 발급 요청을 보냅니다.
     * @param requestDTO authKey, customerKey를 포함해야 합니다.
     * @return 빌링키 발급 성공
     *         - 등록된 카드 정보와 발급된 billingKey가 포함되어 있는 Billing 객체가 돌아옵니다.
     *         빌링키 발급 실패
     *         - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> issueBillingKey(BillingKeyRequestDTO requestDTO) throws IOException {
        ObjectNode requestData = JsonUtil.parseObjectNode(requestDTO);
        Map<String, Object> response = tossPaymentStrategy.sendBillingKeyRequest(requestData);
        return response;
    }

    /**
     * 토스페이먼츠 api 서버로 브랜드페이 결제 승인 요청을 보냅니다.
     * paymentKey에 해당하는 결제를 인증하고 승인합니다. Basic 인증 방식을 사용합니다.
     * @param requestDTO paymentKey, amount, customerKey, orderId 를 포함해야 합니다.
     * @return 결제 승인에 성공했다면 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     *         결제 승인에 실패했다면 HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     * @throws IOException
     */
    @Override
    public Map<String, Object> confirmBrandpay(BrandpayRequestDTO requestDTO) throws IOException {
        ObjectNode requestData = JsonUtil.parseObjectNode(requestDTO);
        Map<String, Object> response = tossPaymentStrategy.sendBrandpayRequest(requestData);
        return response;
    }
}
