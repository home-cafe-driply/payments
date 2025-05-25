package com.driply.payments.payment.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class PaymentRequestDTO {
    private String pgType;
    private String orderId;
    private BigDecimal amount;

    public abstract Map<String, Object> getModuleSpecificData();
}
