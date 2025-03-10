package com.driply.payments.payment.controller;

import com.driply.payments.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

@Controller
//@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;


    /**
     * 위젯 결제와 일반결제 요청을 처리합니다.
     * @param request 결제 요청 정보를 전달 받습니다.
     * @param jsonBody 결제사별로 api 요청에 필요한 데이터를 담고 있습니다.
     * @return 결제사의 응답 결과를 바탕으로 200(결제 승인 완료) 혹은 400(결제 승인 실패) status code를 포함한 응답을 반화합니다.
     * @throws IOException 예외 처리 구현해야 합니다. 추후 수정 예정.
     */
    @PostMapping(value = {"/confirm/widget", "/confirm/payment"})
    public ResponseEntity<Map<String, Object>> confirmPayment(HttpServletRequest request, @RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.processPayment(request.getRequestURI(), jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    /**
     * 정기결제를 수행합니다.
     * @param jsonBody 정기결제 요청에 필요한 데이터를 포함합니다.
     * @return 정기결제 성공 여부에 따라 200(정기결제 성공) 혹은 400(정기결제 실패) status code를 포함한 응답을 반환합니다.
     * @throws IOException
     */
    @RequestMapping(value = "/confirm-billing")
    public ResponseEntity<Map<String, Object>> confirmBilling(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.confirmBilling(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    /**
     * 빌링키를 발급합니다. PG사 api로 빌링키 발급 요청을 처리합니다.
     * @param jsonBody 빌링키 발급에 필요한 데이터를 포함합니다.
     * @return 빌링키 발급 성공 여부에 따라 200(빌링키 발급 성공) 혹은 400(빌링키 발급 실패) status code를 포함한 응답을 반환합니다.
     * @throws IOException
     */
    @RequestMapping(value = "/issue-billing-key")
    public ResponseEntity<Map<String, Object>> issueBillingKey(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.issueBillingKey(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    /**
     * 브랜드페이 결제 과정에서 필요한 Access Token 발급 요청을 처리합니다.
     * @param customerKey 상점에서 만든 고객의 고유 ID입니다.
     * @param code Access Token 발급에 필요한 Authorization Code(임시 인증 코드)입니다.
     * @return 인증 성공 여부에 따라 200(인증 성공) 혹은 400(인증 실패) status code를 포함한 응답을 반환합니다.
     * @throws IOException
     */
    @GetMapping(value = "/callback-auth")
    public ResponseEntity<Map<String, Object>> callbackAuth(@RequestParam String customerKey, @RequestParam String code) throws IOException {
        Map<String, Object> response = paymentService.customerAuthorization(customerKey, code);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    /**
     * 브랜드페이 결제 승인 요청을 처리합니다.
     * @param jsonBody 결제 승인 요청에 필요한 데이터를 포함합니다.
     * @return 결제 승인 성공 여부에 따라 200(결제 성공) 혹은 400(결제 실패) status를 포함한 응답 결과를 반환합니다.
     * @throws IOException
     */
    @PostMapping(value = "/confirm/brandpay", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> confirmBrandpay(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.confirmBrandpay(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    /**
     * root 엔드포인트에 대한 접근을 처리합니다.
     * @return 위젯 결제 템플릿을 반환합니다. 위젯 결제 템플릿에는 브랜드페이 결제, 일반결제를 위한 템플릿과 연결되어 있습니다.
     */
    @GetMapping(value = "/")
    public String index() {
        return "/widget/checkout";
    }

    /**
     * 요청 실패 결과를 처리합니다.
     * @param request 요청 정보를 담고 있습니다.
     * @param model 실패에 대한 정보를 담을 수 있는 모델 객체입니다.
     * @return 실패 코드와 메시지를 모델에 포함하여 실패 템플릿을 반환합니다.
     */
    @GetMapping(value = "/fail")
    public String failPayment(HttpServletRequest request, Model model) {
        model.addAttribute("code", request.getParameter("code"));
        model.addAttribute("message", request.getParameter("message"));
        return "/fail";
    }
}
