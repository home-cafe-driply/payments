package com.driply.payments.config;

import java.net.InetSocketAddress;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class IpWhitelistFilter implements WebFilter {
	private static final Set<String> ALLOWED_IPS = Set.of(
		"13.124.18.147",
		"13.124.108.35",
		"3.36.173.151",
		"3.38.81.32",
		"115.92.221.121",
		"115.92.221.122",
		"115.92.221.125",
		"115.92.221.126",
		"115.92.221.123",
		"115.92.221.127"
	);

	private static final Set<String> PROTECTED_PATHS = Set.of(
		"/api/v1/payment/confirm/payment",
		"/api/v1/payment/confirm/widget",
		"/api/v1/payment/callback"
	);

	@NonNull
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (PROTECTED_PATHS.contains(path)) {
			InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
			String remoteIp = (remoteAddress != null) ? remoteAddress.getAddress().getHostAddress() : "UNKNOWN";
			if (!ALLOWED_IPS.contains(remoteIp)) {
				exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
				return exchange.getResponse().setComplete();
			}
		}
		return chain.filter(exchange);
	}
}
