package com.example.calculadora.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

/**
 * Representa um intervalo de investimento com aporte mensal fixo,
 * duração e aporte extra opcional injetado no início do período.
 */
@Value
@Builder
@Jacksonized
public class SimulationInterval {

    /** Valor aportado mensalmente durante o intervalo. */
    BigDecimal monthlyContribution;

    /** Duração do intervalo em meses. */
    int periodInMonths;

    /** Aporte extra injetado no início do intervalo (opcional). */
    BigDecimal extraContribution;
}
