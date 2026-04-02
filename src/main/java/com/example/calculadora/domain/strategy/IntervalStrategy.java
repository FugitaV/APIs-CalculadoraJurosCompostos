package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;

import java.math.BigDecimal;

/**
 * Contrato do padrão Strategy para cálculo de um intervalo de investimento.
 */
public interface IntervalStrategy {

    /**
     * Calcula o resultado de um intervalo.
     *
     * @param currentBalance saldo inicial do intervalo
     * @param interval       dados do intervalo (aporte, duração, extra)
     * @param monthlyRate    taxa mensal decimal (ex: 0.01 para 1 % a.m.)
     * @return resultado com saldo final, total aportado e juros gerados
     */
    IntervalResult calculate(BigDecimal currentBalance,
                             SimulationInterval interval,
                             BigDecimal monthlyRate);
}
