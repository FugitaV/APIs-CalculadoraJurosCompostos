package com.example.calculadora.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/** Resultado completo de uma simulação de juros compostos. */
@Value
@Builder
public class SimulationResult {

    /** Valor inicial + soma de todos os aportes (mensais e extras). */
    BigDecimal totalInvested;

    /** Total de juros acumulados = finalValue − totalInvested. */
    BigDecimal totalInterest;

    /** Saldo final após todos os intervalos. */
    BigDecimal finalValue;

    /** Detalhamento por intervalo. */
    List<IntervalResult> intervals;
}
