package com.example.calculadora.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Resultado calculado para um único intervalo de investimento. */
@Value
@Builder
public class IntervalResult {

    /** Saldo ao final do intervalo (torna-se saldo inicial do próximo). */
    BigDecimal finalBalance;

    /** Total de aportes realizados no intervalo (mensal × meses + extra). */
    BigDecimal totalContributed;

    /** Juros gerados no intervalo = finalBalance − saldoInicial − totalContributed. */
    BigDecimal interestEarned;
}
