package com.example.calculadora.application.port;

import com.example.calculadora.adapter.in.ExportResponse;

/** Porta de entrada: gera o Excel de uma simulação dado o seu ID. */
public interface ExportSimulationUseCase {

    ExportResponse export(String simulationId);
}
