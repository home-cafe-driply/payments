package com.driply.payments.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingRequestDTO {
    private String customerKey;
    private Double amount;
    private String orderId;
    private String orderName;
    private String customerEmail;
    private String customerName;
}
