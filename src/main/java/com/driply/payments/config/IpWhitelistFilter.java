package com.driply.payments.config;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * 특정 API 경로에 대해 허용된 IP만 접근할 수 있도록 제한하는 화이트리스트 WebFilter입니다.
 * <p>
 * 지정된 경로(PROTECTED_PATHS)에 대한 요청이 들어올 때,
 * 1차로 실제 TCP 연결의 원격 IP가 허용 목록(ALLOWED_IPS)에 포함되어 있는지 확인합니다.
 * 만약 프록시 서버를 통한 접근이 예상되는 경우(PROXY_IPS에 해당 IP가 포함된 경우),
 * 2차로 X-Forwarded-For 헤더에서 추출한 원본 IP가 허용 목록에 포함되어 있는지 검사합니다.
 * <p>
 * 두 조건을 모두 만족하지 않으면 403 Forbidden 응답을 반환하며, 허용된 경우에만 다음 필터로 요청을 전달합니다.
 * <p>
 * <b>주의:</b> X-Forwarded-For 헤더는 신뢰할 수 없는 환경에서는 조작될 수 있으므로,
 * 반드시 신뢰할 수 있는 프록시 환경에서만 사용해야 합니다.
 *
 * @author havegrit
 */
@Component
public class IpWhitelistFilter implements WebFilter {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
		"/api/v1/payment/confirm/payment",
		"/api/v1/payment/confirm/widget",
		"/api/v1/payment/callback"
	);

	/**
	 * 요청이 보호된 경로(PROTECTED_PATHS)에 해당하는 경우,
	 * 허용된 IP(ALLOWED_IPS)인지 검증합니다.
	 * <ul>
	 *     <li>1차: 실제 TCP 연결된 IP가 허용 IP에 포함되는지 확인</li>
	 *     <li>2차: 프록시 서버인 경우 X-Forwarded-For 헤더의 IP가 허용 IP에 포함되는지 확인</li>
	 *     <li>둘 다 아니면 403 Forbidden 반환</li>
	 * </ul>
	 *
	 * @param exchange 요청 및 응답 정보를 담은 ServerWebExchange
	 * @param chain    다음 WebFilterChain
	 * @return Mono<Void> WebFlux 비동기 체인
	 */
	@NonNull
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (PROTECTED_PATHS.contains(path)) {
			String remoteIp = extractRemoteIp(exchange);
			logger.info("1차 검증 - 원격 IP: {}", remoteIp);

			// 1차 검증: 직접 접속 IP 허용 여부
			if (ALLOWED_IPS.contains(remoteIp)) {
				return chain.filter(exchange);
			}

			// 2차 검증: 프록시 서버인 경우
			if (PROXY_IPS.contains(remoteIp)) {
				String xffIp = extractXFFHeader(exchange);
				logger.info("2차 검증 - XFF IP: {}", xffIp);

				if (xffIp != null && ALLOWED_IPS.contains(xffIp)) {
					return chain.filter(exchange);
				}
			}

			logger.warn("차단된 접근 - 원격 IP: {}", remoteIp);
			exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
			return exchange.getResponse().setComplete();
		}
		return chain.filter(exchange);
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
}
