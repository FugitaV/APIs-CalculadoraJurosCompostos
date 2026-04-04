package com.example.calculadora;

import com.example.calculadora.adapter.in.CalculationRequest;
import com.example.calculadora.adapter.in.CalculationResponse;
import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.SimulationInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula uma requisição do API Gateway chegando na Lambda localmente.
 *
 * Execução:
 *   mvn test -Dtest=LambdaGatewaySimulationTest -pl .
 *
 * O DynamoDB é substituído por um mock — nenhuma conexão AWS é necessária.
 */
@SpringBootTest
class LambdaGatewaySimulationTest {

    // Substitui os beans AWS por mocks — sem conexão real ao DynamoDB
    @MockBean SaveSimulationPort saveSimulationPort;
    @MockBean DynamoDbClient dynamoDbClient;
    @MockBean DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    Function<Message<CalculationRequest>, CalculationResponse> calculationFunction;

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void simulateApiGatewayRequest() throws Exception {
        // ── monta o payload exatamente como o API Gateway enviaria ─────────────
        CalculationRequest request = CalculationRequest.builder()
                .initialValue(new BigDecimal("10000.00"))
                .annualRate(new BigDecimal("12.00"))
                .intervals(List.of(
                        SimulationInterval.builder()
                                .monthlyContribution(new BigDecimal("500.00"))
                                .periodInMonths(12)
                                .extraContribution(null)
                                .build(),
                        SimulationInterval.builder()
                                .monthlyContribution(new BigDecimal("1000.00"))
                                .periodInMonths(24)
                                .extraContribution(new BigDecimal("10000.00"))
                                .build()
                ))
                .build();

        // ── headers que o API Gateway injeta (X-User-Id via authorizer/header) ─
        Message<CalculationRequest> gatewayMessage = MessageBuilder
                .withPayload(request)
                .setHeader("X-User-Id", "usuario-teste-123")
                .setHeader("Content-Type", "application/json")
                .build();

        // ── invoca a função ────────────────────────────────────────────────────
        CalculationResponse response = calculationFunction.apply(gatewayMessage);

        // ── imprime o resultado formatado no console ──────────────────────────
        System.out.println("\n========== RESPOSTA DA LAMBDA ==========");
        System.out.println(mapper.writeValueAsString(response));
        System.out.println("========================================\n");

        // ── asserções básicas ─────────────────────────────────────────────────
        assertThat(response).isNotNull();
        assertThat(response.getIntervals()).hasSize(2);
        assertThat(response.getFinalValue())
                .isGreaterThan(response.getTotalInvested());
        assertThat(response.getTotalInterest()).isPositive();
        assertThat(response.getTotalInvested())
                .isEqualByComparingTo(
                        new BigDecimal("10000")           // initialValue
                        .add(new BigDecimal("6000"))      // 500 × 12
                        .add(new BigDecimal("24000"))     // 1000 × 24
                        .add(new BigDecimal("10000"))     // extraContribution
                );
    }
}
