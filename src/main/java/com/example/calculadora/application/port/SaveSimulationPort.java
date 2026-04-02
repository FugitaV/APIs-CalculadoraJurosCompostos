package com.example.calculadora.application.port;

import com.example.calculadora.domain.model.Simulation;

/** Porta de saída: persiste uma simulação calculada. */
public interface SaveSimulationPort {

    void save(Simulation simulation);
}
