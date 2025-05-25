package com.driply.payments.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.driply.payments.annotation.Retry;

@Aspect
@Component
public class RetryAspect {

	@Around("@annotation(retry)")
	public Object doRetry(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
		int maxRetry = retry.value();
		Exception exceptionHolder = null;

		for (int attempt = 1; attempt <= maxRetry; attempt++) {
			try {
				return joinPoint.proceed();
			} catch (Exception e) {
				exceptionHolder = e;
				if (attempt == maxRetry) {
					throw exceptionHolder;
				}
			}
		}
		throw new IllegalStateException("재시도 로직에서 예기치 않은 종료");
	}
}
