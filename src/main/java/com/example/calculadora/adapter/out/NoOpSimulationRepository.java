package com.example.calculadora.adapter.out;

import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.Simulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Implementação no-op do SaveSimulationPort usada no perfil "local".
 * Apenas loga a simulação sem tentar conectar ao DynamoDB.
 */
@Repository
@Profile("local")
public class NoOpSimulationRepository implements SaveSimulationPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpSimulationRepository.class);

    @Override
    public void save(Simulation simulation) {
        log.info("[local] Simulação não persistida (perfil local). id={}, userId={}, finalValue={}",
                simulation.getSimulationId(),
                simulation.getUserId(),
                simulation.getFinalValue());
    }
}
