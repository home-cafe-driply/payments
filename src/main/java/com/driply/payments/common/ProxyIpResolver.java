package com.driply.payments.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyIpResolver {

	/**
	 * 주어진 도메인명 또는 IP 문자열 배열을 받아 각 호스트의 IP 주소를 모두 조회하여 반환합니다.
	 *
	 * @param hosts 도메인명 또는 IP 주소 문자열 배열
	 * @return 모든 호스트의 IP 주소를 담은 중복 없는 Set
	 *
	 * <p>호스트를 찾을 수 없는 경우 에러 로그를 남기고 다음 호스트로 진행합니다.</p>
	 */
	public static Set<String> resolveProxyIps(String... hosts) {
		Set<String> proxyIps = new HashSet<>();
		for (String host : hosts) {
			try {
				InetAddress[] addresses = InetAddress.getAllByName(host);
				for (InetAddress addr : addresses) {
					proxyIps.add(addr.getHostAddress());
				}
			} catch (UnknownHostException e) {
				log.error("호스트를 찾을 수 없습니다: {}", host);
			}
		}
		return proxyIps;
	}
}
