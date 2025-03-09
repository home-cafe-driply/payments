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

    @PostMapping(value = {"/confirm/widget", "/confirm/payment"})
    public ResponseEntity<Map<String, Object>> confirmPayment(HttpServletRequest request, @RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.processPayment(request.getRequestURI(), jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @RequestMapping(value = "/confirm-billing")
    public ResponseEntity<Map<String, Object>> confirmBilling(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.confirmBilling(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @RequestMapping(value = "/issue-billing-key")
    public ResponseEntity<Map<String, Object>> issueBillingKey(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.issueBillingKey(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @GetMapping(value = "/callback-auth")
    public ResponseEntity<Map<String, Object>> callbackAuth(@RequestParam String customerKey, @RequestParam String code) throws IOException {
        Map<String, Object> response = paymentService.customerAuthorization(customerKey, code);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @PostMapping(value = "/confirm/brandpay", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> confirmBrandpay(@RequestBody String jsonBody) throws IOException {
        Map<String, Object> response = paymentService.confirmBrandpay(jsonBody);
        return ResponseEntity.status(response.containsKey("error") ? 400 : 200).body(response);
    }

    @GetMapping(value = "/")
    public String index() {
        return "/widget/checkout";
    }

    @GetMapping(value = "/fail")
    public String failPayment(HttpServletRequest request, Model model) {
        model.addAttribute("code", request.getParameter("code"));
        model.addAttribute("message", request.getParameter("message"));
        return "/fail";
    }
}
