package com.driply.payments.payment.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.entity.PGType;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.entity.PaymentStatus;
import com.driply.payments.payment.gateway.PaymentGateway;
import com.driply.payments.payment.gateway.PaymentGatewayFactory;
import com.driply.payments.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final PaymentRepository paymentRepository;

    /**
     * 토스페이먼츠사 api를 통해 결제 승인 요청을 보내기 위해 데이터 전처리하여 sendPaymentRequest() 메소드를 호출합니다.
     *
     * @param requestDTO paymentKey, orderId, amount, requestUri 값을 포함해야 합니다.
     * @return 결제 승인 성공
     * - 결제 정보를 담고 있는 Payment 객체가 돌아옵니다.
     * - 결제 한 건의 결제 상태, 결제 취소 기록, 매출 전표, 현금영수증 정보 등을 포함합니다.
     * - 객체의 구성은 결제수단(카드, 가상계좌, 간편결제 등)에 따라 조금씩 달라집니다.
     * 결제 승인 실패
     * - HTTP 상태 코드와 함께 에러 객체가 돌아옵니다.
     */
    @Override
    @Transactional
    public Map<String, Object> processPayment(PaymentRequestDTO requestDTO) {
        logger.info("payment processing: {}", requestDTO);
        Map<String, Object> response = null;
        try {
            Payment payment = paymentRepository.save(
                    //TODO: InvalidPGTypeException 예외 처리
                    Payment.builder()
                            .pgType(PGType.valueOf(requestDTO.getPgType()))
                            .orderId(requestDTO.getOrderId())
                            .amount(requestDTO.getAmount())
                            .status(PaymentStatus.PENDING)
                            .requestedAt(OffsetDateTime.now())
                            .extraData(requestDTO.getModuleSpecificData())
                            .build()
            );
            PaymentGateway paymentGateway = paymentGatewayFactory.getGateway(requestDTO.getPgType());

            Long paymentId = payment.getPaymentId();
            response = paymentGateway.processPayment(requestDTO, paymentId);
            logger.info("response data: {}", response);
            payment.approve(response.get("type").toString());
        } catch (Exception e) {
            logger.error("Payment failed", e);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다: " + paymentId));
    }

    /**
     * PENDING 상태의 결제 객체를 생성합니다.
     * @param requestDTO 결제 요청에 대한 메타 데이터를 포함합니다.
     * @return PENDING 상태의 Payment 객체를 반환합니다.
     */
    private Payment createPendingPayment(PaymentRequestDTO requestDTO) {
        return Payment.builder()
            .pgType(PGType.valueOf(requestDTO.getPgType()))
            .orderId(requestDTO.getOrderId())
            .amount(requestDTO.getAmount())
            .status(PaymentStatus.PENDING)
            .extraData(requestDTO.getModuleSpecificData())
            .requestedAt(OffsetDateTime.now())
            .build();
    }
}
