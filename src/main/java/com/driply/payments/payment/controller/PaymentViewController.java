package com.driply.payments.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Controller
public class PaymentViewController {
	@GetMapping("/")
	public Mono<String> index() {
		return Mono.just("payment/checkout");
	}

	@GetMapping("/api/v1/payment/success")
	public Mono<Rendering> successPayment(ServerWebExchange exchange) {
		return Mono.just(Rendering.view("/payment/success.html")
			.modelAttribute("paymentType", exchange.getRequest().getQueryParams().getFirst("paymentType"))
			.modelAttribute("orderId", exchange.getRequest().getQueryParams().getFirst("orderId"))
			.modelAttribute("paymentKey", exchange.getRequest().getQueryParams().getFirst("paymentKey"))
			.modelAttribute("amount", exchange.getRequest().getQueryParams().getFirst("amount"))
			.build());
	}
	
	@GetMapping("/api/v1/payment/fail")
	public Mono<Rendering> failPayment(ServerWebExchange exchange) {
		return Mono.just(Rendering.view("/fail.html")
			.modelAttribute("code", exchange.getRequest().getQueryParams().getFirst("code"))
			.modelAttribute("message", exchange.getRequest().getQueryParams().getFirst("message"))
			.build());
	}
}
