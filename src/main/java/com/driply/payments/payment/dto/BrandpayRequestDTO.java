package com.driply.payments.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrandpayRequestDTO {
    private String paymentKey;
    private String orderId;
    private Double amount;
    private String customerKey;
}
