package com.driply.payments.payment.gateway;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import com.driply.payments.payment.entity.PGType;

@SpringBootTest
class PaymentGatewayFactoryTest {

    private PaymentGatewayFactory paymentGatewayFactory;
    private WebClient tossWebClient = WebClient.builder()
        .baseUrl("https://api.tosspayments.com")
        .build();

    @BeforeEach
    void setUp() {
        List<PaymentGateway> gateways = List.of(new TossPaymentsGateway(tossWebClient));
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