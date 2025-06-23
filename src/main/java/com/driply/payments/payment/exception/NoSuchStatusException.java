package com.driply.payments.payment.exception;

public class NoSuchStatusException extends RuntimeException {
	public NoSuchStatusException(String status) {
		super("알 수 없는 상태 정보: %s".formatted(status));
	}
}
