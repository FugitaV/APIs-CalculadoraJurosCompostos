package com.example.calculadora.adapter.in;

import com.example.calculadora.application.port.ExportSimulationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;

import java.util.function.Function;

/**
 * Adapter de entrada para exportação de simulação em Excel.
 *
 * <p>Recebe o {@code simulationId} pelo body e devolve o arquivo .xlsx
 * codificado em Base64 junto ao nome sugerido do arquivo.</p>
 *
 * <p>Esta função é registrada como um bean separado e ativada via
 * a variável de ambiente {@code SPRING_CLOUD_FUNCTION_DEFINITION=exportFunction}
 * na segunda Lambda (mesmo JAR, função diferente).</p>
 */
@RequiredArgsConstructor
public class ExportFunction implements Function<Message<ExportRequest>, ExportResponse> {

    private final ExportSimulationUseCase exportSimulationUseCase;

    @Override
    public ExportResponse apply(Message<ExportRequest> message) {
        String simulationId = message.getPayload().getSimulationId();
        return exportSimulationUseCase.export(simulationId);
    }
}
