package com.driply.payments.payment.gateway;

import com.driply.payments.payment.entity.PGType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class PaymentGatewayFactoryTest {

    private PaymentGatewayFactory paymentGatewayFactory;

    @BeforeEach
    void setUp() {
        List<PaymentGateway> gateways = List.of(new TossPaymentsGateway());
        paymentGatewayFactory = new PaymentGatewayFactory(gateways);
    }

    @Test
    @DisplayName("pgType이 TOSS일 때, TossPaymentGateway를 반환")
    void getGateway() {
        String pgType = PGType.TOSS.name();
        PaymentGateway gateway = paymentGatewayFactory.getGateway(pgType);

        Assertions.assertNotNull(gateway);
        Assertions.assertInstanceOf(TossPaymentsGateway.class, gateway);
        TossPaymentsGateway tossPaymentsGateway = (TossPaymentsGateway) gateway;
        Assertions.assertEquals(pgType, tossPaymentsGateway.getPGType());
    }
}