package com.driply.payments.payment.controller;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import com.driply.payments.payment.converter.PaymentRequestConverterFactory;
import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.PaymentResponseDTO;
import com.driply.payments.payment.entity.Payment;
import com.driply.payments.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
//@RestController
//@RequestMapping("/api/v1/payment")
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
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody Map<String, Object> requestBody) throws IOException {
        logger.info("request body: {}", requestBody);
        PaymentRequestDTO requestDTO = converterFactory.convert(requestBody);
        Map<String, Object> response = paymentService.processPayment(requestDTO);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @GetMapping(value = "/payment/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(
                PaymentResponseDTO.builder()
                        .paymentId(paymentId)
                        .build());
    }

    /**
     * root 엔드포인트에 대한 접근을 처리합니다.
     *
     * @return 위젯 결제 템플릿을 반환합니다. 위젯 결제 템플릿은 기본적으로 toss payments에서 제공하는 모든 결제 수단에 대한 ui를 제공하며,
     * 여기에는 브랜드페이 결제, 일반결제를 위한 템플릿과 연결되어 있습니다.
     */
    @GetMapping(value = "/")
    public Mono<View> index() {
        return viewResolver.resolveViewName("/widget/checkout", Locale.getDefault());
    }

    /**
     * 요청 실패 결과를 처리합니다.
     *
     * @param exchange
     * @return 실패 코드와 메시지를 모델에 포함하여 실패 템플릿을 반환합니다.
     */
    @GetMapping(value = "/fail")
    public Mono<Rendering> failPayment(ServerWebExchange exchange) {
        return Mono.just(Rendering.view("/fail.html")
            .modelAttribute("code", exchange.getRequest().getQueryParams().getFirst("code"))
            .modelAttribute("message", exchange.getRequest().getQueryParams().getFirst("message"))
            .build());
    }
}
