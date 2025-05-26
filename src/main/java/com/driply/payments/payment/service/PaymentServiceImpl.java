package com.driply.payments.payment.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResponseDTO;
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
    private final ExecutorService paymentExecutor = Executors.newFixedThreadPool(20);

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
            Payment payment = paymentRepository.saveAndFlush(createPendingPayment(requestDTO));
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

    /**
     * 결제 승인 프로세스를 비동기로 처리합니다.
     * @param requestDTO 결제 요청에 필요한 메타 데이터를 포함합니다.
     * @return paymentId(접수된 결제 엔티티의 pk), status(PENDING 상태의 결제 엔티티 생성), message(결제 접수 응답 메시지), isSuccess(결제 접수 성공 여부)
     */
    @Override
    public PaymentResponseDTO processPaymentAsync(PaymentRequestDTO requestDTO) {
        Payment payment = createPendingPayment(requestDTO);
        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        PaymentGateway paymentGateway = paymentGatewayFactory.getGateway(requestDTO.getPgType());
        Long paymentId = savedPayment.getPaymentId();

        try {
            paymentGateway.processPayment(requestDTO, paymentId);

            return PaymentResponseDTO.builder()
                .paymentId(savedPayment.getPaymentId())
                .status(savedPayment.getStatus())
                .success(true)
                .message("결제 요청이 접수되었습니다.")
                .build();
        } catch (RuntimeException e) {
            logger.error("비동기 결제 처리 중 예외 발생", e);

            return PaymentResponseDTO.builder()
                .paymentId(null)
                .status(PaymentStatus.FAILED)
                .success(false)
                .message(e.getMessage())
                .build();
        }
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
