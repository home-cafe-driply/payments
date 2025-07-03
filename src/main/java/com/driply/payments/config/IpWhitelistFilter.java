package com.driply.payments.config;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 특정 API 경로에 대해 허용된 IP만 접근할 수 있도록 제한하는 {@link org.springframework.web.server.WebFilter} 구현 클래스입니다.
 * <p>
 * 지정된 보호 경로({@code PROTECTED_PATHS})에 대한 HTTP 요청이 발생할 경우,
 * 요청자의 IP가 허용된 목록({@code ALLOWED_IPS})에 포함되어 있는지 검사합니다.
 * <br>
 * 프록시 서버({@code PROXY_IPS})를 경유한 요청의 경우, {@code X-Forwarded-For} 헤더에서 실제 클라이언트 IP를 추출하여 검증합니다.
 * <br>
 * 허용되지 않은 IP의 접근 시, HTTP 403 Forbidden 응답을 반환하며 접근을 차단합니다.
 * <br>
 * 그 외 경로에 대해서는 필터링 없이 체인을 그대로 통과시킵니다.
 * </p>
 * <b>주의:</b> X-Forwarded-For 헤더는 신뢰할 수 없는 환경에서는 조작될 수 있으므로,
 * 반드시 신뢰할 수 있는 프록시 환경에서만 사용해야 합니다.
 *
 * @author havegrit
 */
@Slf4j
@Component
public class IpWhitelistFilter implements WebFilter {
	private static final Set<String> PROXY_IPS = Set.of("");

	private static final Set<String> ALLOWED_IPS = Set.of(
		"127.0.0.1",
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
		"/api/v1/payment/callback"
	);

	/**
	 * 보호된 경로에 대한 IP 기반 접근 제어를 수행하는 필터 메서드입니다.
	 *
	 * @param exchange 요청 및 응답 정보를 담은 ServerWebExchange
	 * @param chain    다음 WebFilterChain
	 * @return Mono<Void> WebFlux 비동기 체인
	 */
	@NonNull
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();

		if (!PROTECTED_PATHS.contains(path)) {
			return chain.filter(exchange);
		}

		String remoteIp = extractRemoteIp(exchange);
		String targetIp = determineTargetIp(exchange, remoteIp);

		if (isAllowedIp(targetIp)) {
			return chain.filter(exchange);
		}

		log.warn("차단된 접근 - 원격 IP: {}", remoteIp);
		exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
		return exchange.getResponse().setComplete();
	}

	/**
	 * ServerWebExchange에서 실제 TCP 연결을 맺은 상대방의 IP 주소를 반환합니다.
	 * <p>
	 * 프록시나 로드밸런서를 거치지 않은 경우, 이 값은 실제 클라이언트의 IP입니다.
	 * 프록시 환경에서는 중간 서버의 IP가 반환될 수 있습니다.
	 * <p>
	 * remoteAddress 또는 IP 정보가 없을 경우 "UNKNOWN"을 반환합니다.
	 *
	 * @param exchange 요청 정보를 담은 ServerWebExchange 객체
	 * @return 실제 연결된 상대방의 IP 주소 또는 "UNKNOWN"
	 */
	private String extractRemoteIp(ServerWebExchange exchange) {
		InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
		return (remoteAddress != null && remoteAddress.getAddress() != null)
			? remoteAddress.getAddress().getHostAddress()
			: "UNKNOWN";
	}

	/**
	 * ServerWebExchange에서 "X-Forwarded-For" 헤더의 첫 번째 IP 주소를 가져옵니다.
	 * <p>
	 * 헤더가 없거나 비어 있으면 "UNKNOWN"을 반환합니다.
	 * <p>
	 * 참고: X-Forwarded-For 헤더는 클라이언트가 조작할 수 있으므로 신뢰에 주의해야 합니다.
	 *
	 * @param exchange 요청 정보를 담은 ServerWebExchange 객체
	 * @return 첫 번째 IP 주소 또는 "UNKNOWN"
	 */
	private String extractXFFHeader(ServerWebExchange exchange) {
		String xffHeader = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
		if (xffHeader == null || xffHeader.isBlank())
			return "UNKNOWN";

		return Arrays.stream(xffHeader.split(","))
			.map(String::trim)
			.filter(ip -> !ip.isEmpty())
			.findFirst()
			.orElse("UNKNOWN");
	}

	/**
	 * 요청이 프록시를 통해 온 것인지 직접 온 것인지 판단하여 검증할 대상 IP를 결정합니다.
	 * 프록시 요청인 경우 X-Forwarded-For 헤더에서 클라이언트 IP를 추출하고,
	 * 직접 요청인 경우 원격 IP를 그대로 사용합니다.
	 *
	 * @param exchange 요청 정보를 담은 ServerWebExchange 객체
	 * @param remoteIp 요청자의 원격 IP 주소
	 * @return 검증할 대상 IP 주소
	 */
	private String determineTargetIp(ServerWebExchange exchange, String remoteIp) {
		if (isProxy(remoteIp)) {
			String clientIp = extractXFFHeader(exchange);
			log.info("프록시 요청 - Proxy IP: {}, Client IP(XFF): {}", remoteIp, clientIp);
			return clientIp;
		} else {
			log.info("직접 요청 - Remote IP: {}", remoteIp);
			return remoteIp;
		}
	}

	/**
	 * 주어진 IP 주소가 프록시 서버의 IP인지 확인합니다.
	 *
	 * @param remoteIp 확인할 원격 IP 주소
	 * @return IP가 프록시 목록에 포함되어 있으면 true, 그렇지 않으면 false
	 */
	private boolean isProxy(String remoteIp) {
		return PROXY_IPS.contains(remoteIp);
	}

	/**
	 * 주어진 IP 주소가 허용된 IP 목록에 포함되어 있는지 확인합니다.
	 *
	 * @param ip 확인할 IP 주소
	 * @return IP가 허용된 목록에 포함되어 있으면 true, 그렇지 않으면 false
	 */
	private boolean isAllowedIp(String ip) {
		return ip != null && ALLOWED_IPS.contains(ip);
	}
}
