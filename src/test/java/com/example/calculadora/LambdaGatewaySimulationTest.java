package com.example.calculadora;

import com.example.calculadora.adapter.in.CalculationFunction;
import com.example.calculadora.adapter.in.CalculationRequest;
import com.example.calculadora.adapter.in.CalculationResponse;
import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.application.service.CompoundInterestService;
import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula uma requisição do API Gateway chegando na Lambda — sem contexto Spring,
 * sem conexão AWS. Todas as dependências são instanciadas diretamente.
 *
 * Execução:
 *   mvn test -Dtest=LambdaGatewaySimulationTest
 */
class LambdaGatewaySimulationTest {

    private CalculationFunction calculationFunction;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @BeforeEach
    void setUp() {
        // DynamoDB mockado — zero conexão AWS
        SaveSimulationPort mockRepository = Mockito.mock(SaveSimulationPort.class);
        CompoundInterestService service = new CompoundInterestService(mockRepository);
        calculationFunction = new CalculationFunction(service);
    }

    @Test
    void simulateApiGatewayRequest() throws Exception {

        // ── payload exatamente como o API Gateway enviaria ────────────────────
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

        // ── headers que o API Gateway injeta ─────────────────────────────────
        Message<CalculationRequest> gatewayMessage = MessageBuilder
                .withPayload(request)
                .setHeader("X-User-Id", "usuario-teste-123")
                .setHeader("Content-Type", "application/json")
                .build();

        // ── invoca a função ───────────────────────────────────────────────────
        CalculationResponse response = calculationFunction.apply(gatewayMessage);

        // ── imprime o resultado formatado ─────────────────────────────────────
        System.out.println("\n========== RESPOSTA DA LAMBDA ==========");
        System.out.println(mapper.writeValueAsString(response));
        System.out.println("========================================\n");

        // ── asserções ─────────────────────────────────────────────────────────
        assertThat(response).isNotNull();
        assertThat(response.getIntervals()).hasSize(2);
        assertThat(response.getFinalValue()).isGreaterThan(response.getTotalInvested());
        assertThat(response.getTotalInterest()).isPositive();

        // totalInvested = 10000 + 6000 (500×12) + 24000 (1000×24) + 10000 (extra)
        assertThat(response.getTotalInvested())
                .isEqualByComparingTo(new BigDecimal("50000"));

        // cada intervalo tem saldo final e juros positivos
        for (IntervalResult interval : response.getIntervals()) {
            assertThat(interval.getFinalBalance()).isPositive();
            assertThat(interval.getInterestEarned()).isPositive();
        }
    }
}
