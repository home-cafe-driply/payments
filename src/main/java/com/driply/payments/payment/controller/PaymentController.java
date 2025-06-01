package com.driply.payments.payment.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.result.view.ViewResolver;

import com.driply.payments.payment.converter.PaymentRequestConverterFactory;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResponseDTO;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ViewResolver viewResolver;
    private final PaymentService paymentService;
    private final PaymentRequestConverterFactory converterFactory;

    /**
     * 위젯 결제와 일반결제 요청을 처리합니다.
     *
     * @param requestBody 결제사별로 api 요청에 필요한 데이터를 담고 있습니다.
     * @return 결제사의 응답 결과를 바탕으로 200(결제 승인 완료) 혹은 400(결제 승인 실패) status code를 포함한 응답을 반화합니다.
     */
    @PostMapping(value = {"/confirm/widget", "/confirm/payment"})
    public Mono<ResponseEntity<PaymentResponseDTO>> confirmPayment(@RequestBody Map<String, Object> requestBody) {
        logger.info("request body: {}", requestBody);
        PaymentRequestDTO requestDTO = converterFactory.convert(requestBody);
        PaymentResponseDTO responseDTO = paymentService.processPaymentAsync(requestDTO);
        return Mono.just(ResponseEntity.status(responseDTO.isSuccess() ? 200 : 400).body(responseDTO));
    }

    @GetMapping("/{paymentId}")
    public Mono<ResponseEntity<PaymentResponseDTO>> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);

        return Mono.just(ResponseEntity.ok(
            PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId())
                .build())
        );
    }
}
