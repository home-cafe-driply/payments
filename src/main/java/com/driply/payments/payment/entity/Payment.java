package com.driply.payments.payment.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.Type;

import com.driply.payments.common.BaseEntity;
import com.driply.payments.payment.dto.PGType;
import com.driply.payments.payment.dto.PaymentStatus;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PGType pgType;

	@Column(nullable = false)
	private String orderId;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	private String pgTransactionId;

	private String paymentMethod;

	@Column(nullable = false)
	private OffsetDateTime requestedAt;

	private OffsetDateTime approvedAt;

	@Type(JsonBinaryType.class)
	@Column(columnDefinition = "jsonb", name = "extra_data")
	private Map<String, Object> extraData;

	@Version
	private Long version;

	private boolean isValidTransition(PaymentStatus from, PaymentStatus to) {
		return switch (from) {
			case PENDING -> to == PaymentStatus.PAID || to == PaymentStatus.FAILED || to == PaymentStatus.CANCELED;
			case PAID -> to == PaymentStatus.REFUNDED;
			case FAILED, CANCELED, REFUNDED -> false; // 최종 상태에서는 변경 불가
			default -> false;
		};
	}

	public void approve(String pgTransactionId, String paymentMethod, OffsetDateTime approvedAt) {
		if (!isValidTransition(this.status, PaymentStatus.PAID)) {
			throw new IllegalStateException("이미 처리된 결제입니다.");
		}
		this.pgTransactionId = pgTransactionId;
		this.paymentMethod = paymentMethod;
		this.status = PaymentStatus.PAID;
		this.approvedAt = approvedAt;

	}

	public void fail() {
		if (!isValidTransition(this.status, PaymentStatus.FAILED)) {
			throw new IllegalStateException("이미 처리된 결제입니다.");
		}
		this.status = PaymentStatus.FAILED;
	}

	public void cancel() {
		if (!isValidTransition(this.status, PaymentStatus.CANCELED)) {
			throw new IllegalStateException("결제 승인 상태에서만 취소할 수 있습니다.");
		}
		this.status = PaymentStatus.CANCELED;
	}

	public void refund() {
		if (!isValidTransition(this.status, PaymentStatus.REFUNDED)) {
			throw new IllegalStateException("결제 승인 상태에서만 취소할 수 있습니다.");
		}
		this.status = PaymentStatus.REFUNDED;
	}
}
