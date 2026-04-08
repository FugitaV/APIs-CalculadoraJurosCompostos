package com.example.calculadora.application.port;

import com.example.calculadora.domain.model.Simulation;

import java.util.Optional;

/** Porta de saída: busca uma simulação persistida pelo seu ID. */
public interface GetSimulationPort {

    Optional<Simulation> getById(String simulationId);
}
