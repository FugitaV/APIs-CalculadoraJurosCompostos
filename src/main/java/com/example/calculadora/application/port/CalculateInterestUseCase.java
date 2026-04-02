package com.example.calculadora.application.port;

import com.example.calculadora.domain.model.SimulationInterval;
import com.example.calculadora.domain.model.SimulationResult;

import java.math.BigDecimal;
import java.util.List;

/** Porta de entrada: executa o cálculo de juros compostos e persiste a simulação. */
public interface CalculateInterestUseCase {

    /**
     * @param initialValue valor inicial investido
     * @param annualRate   taxa anual em percentual (ex: 12.00 para 12 % a.a.)
     * @param intervals    lista de intervalos de aporte
     * @param userId       identificador do usuário (vem do header X-User-Id)
     * @return resultado com totais e detalhamento por intervalo
     */
    SimulationResult calculate(BigDecimal initialValue,
                               BigDecimal annualRate,
                               List<SimulationInterval> intervals,
                               String userId);
}
