package com.driply.payments.payment.gateway;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
import com.fasterxml.jackson.databind.ObjectMapper;

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
    @Value("${toss.payments.payment.req.url}")
    private String paymentUrl;

    /**
     * 결제 요청을 비동기적으로 처리합니다.
     * <p>
     * 외부 결제 API(Toss 등)에 비동기 POST 요청을 보내고, 결과는 콜백(subscribe)에서 처리합니다.
     * 성공 시 단순 로깅하며, 실패 시 에러 로그를 남깁니다.
     * 결제 결과 처리는 별도의 콜백(웹훅 등)에서 수행하는 구조입니다.
     *
     * @param requestDTO 결제 요청 데이터 DTO
     * @param paymentId  결제 식별자(로깅 및 추적용)
     * @throws RuntimeException 결제 요청 초기화에 실패한 경우
     */
    @Override
    public void processPayment(PaymentRequestDTO requestDTO, long paymentId) {
        try {
            tossWebClient.post()
                .uri(paymentUrl)
                .header("Authorization", "Basic " + createAuthHeader(API_SECRET_KEY))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    result -> {
                        logger.info("결제 요청 성공: paymentId={}", paymentId);
                    },
                    error -> {
                        logger.error("결제 요청 실패: paymentId={}", paymentId, error);
                    }
                );

            logger.info("결제 요청 시작됨: paymentId={}", paymentId);

        } catch (Exception e) {
            logger.error("결제 요청 초기화 실패: paymentId={}", paymentId, e);
            throw new RuntimeException("결제 요청을 시작할 수 없습니다.", e);
        }
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
     * 결제 API 인증 헤더를 생성합니다.
     * <p>
     * secretKey를 Base64 인코딩하여 Basic 인증 헤더 포맷으로 반환합니다.
     * @param secretKey 결제 API 시크릿 키
     * @return Base64 인코딩된 인증 헤더 값
     */
    private String createAuthHeader(String secretKey) {
        return Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
