package com.driply.payments.payment.converter;

import com.driply.payments.payment.dto.PaymentRequestDTO;
import com.driply.payments.payment.dto.TossPaymentRequestDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class PaymentRequestConverterFactoryTest {

    private PaymentRequestConverterFactory paymentRequestConverterFactory;

    @BeforeEach
    void setUp() {
        paymentRequestConverterFactory = new PaymentRequestConverterFactory(List.of(new TossPaymentsRequestConverter()));
    }

    @Test
    @DisplayName("pgType이 TOSS일 때, TossPaymentRequestDTO로 변환 테스트")
    void convertToTossPaymentRequestDTO() {
        String pgType = "TOSS";
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("paymentKey", "testPaymentKey");
        requestData.put("amount", 10000);
        requestData.put("orderId", "testOrderId");

        PaymentRequestDTO dto = paymentRequestConverterFactory.convert(requestData);
        Assertions.assertNotNull(dto);
        Assertions.assertInstanceOf(TossPaymentRequestDTO.class, dto);
        TossPaymentRequestDTO convertedDTO = (TossPaymentRequestDTO) dto;
        Assertions.assertEquals(pgType, convertedDTO.getPgType());
        Assertions.assertEquals("testPaymentKey", convertedDTO.getPaymentKey());
        Assertions.assertEquals(new BigDecimal(10000), convertedDTO.getAmount());
        Assertions.assertEquals("testOrderId", convertedDTO.getOrderId());
    }
}