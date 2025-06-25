package com.driply.payments.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제 정보를 조회할 때 사용되는 조건을 담는 데이터 전달 객체입니다.
 * <p>
 * 하나 이상의 값을 설정하여 결제 대행사(PG)에서 결제 내역을 조회할 때 사용합니다.
 * PG사 추가시 조회 조건을 필드에 추가하여 사용합니다.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQuery {
	/**
	 * 토스페이먼츠에서 발급하는 결제 고유 식별 값
	 */
	private String paymentKey;
}
