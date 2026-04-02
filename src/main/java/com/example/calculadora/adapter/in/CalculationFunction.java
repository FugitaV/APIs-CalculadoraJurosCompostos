package com.example.calculadora.adapter.in;

import com.example.calculadora.application.port.CalculateInterestUseCase;
import com.example.calculadora.domain.model.SimulationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;

import java.util.function.Function;

/**
 * Adapter de entrada: recebe o evento do API Gateway via Spring Cloud Function,
 * delega o cálculo ao use case e devolve a resposta serializada.
 *
 * <p>A assinatura {@code Function<Message<CalculationRequest>, CalculationResponse>}
 * é necessária para acessar o header {@code X-User-Id} injetado pelo API Gateway.</p>
 */
@RequiredArgsConstructor
public class CalculationFunction implements Function<Message<CalculationRequest>, CalculationResponse> {

    private final CalculateInterestUseCase calculateInterestUseCase;

    @Override
    public CalculationResponse apply(Message<CalculationRequest> message) {
        CalculationRequest request = message.getPayload();
        String userId = resolveUserId(message);

        SimulationResult result = calculateInterestUseCase.calculate(
                request.getInitialValue(),
                request.getAnnualRate(),
                request.getIntervals(),
                userId
        );

        return CalculationResponse.builder()
                .totalInvested(result.getTotalInvested())
                .totalInterest(result.getTotalInterest())
                .finalValue(result.getFinalValue())
                .intervals(result.getIntervals())
                .build();
    }

    /**
     * Extrai o userId do header {@code X-User-Id} (ou sua versão em minúsculas
     * gerada pelo API Gateway HTTP API).
     */
    private String resolveUserId(Message<CalculationRequest> message) {
        Object headerValue = message.getHeaders().get("X-User-Id");
        if (headerValue == null) {
            headerValue = message.getHeaders().get("x-user-id");
        }
        return headerValue != null ? headerValue.toString() : "anonymous";
    }
}
