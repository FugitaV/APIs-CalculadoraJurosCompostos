package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraContributionStrategyTest {

    private final ExtraContributionStrategy strategy = new ExtraContributionStrategy();

    /**
     * Valida que o aporte extra é somado ao saldo ANTES do cálculo e contabilizado
     * no totalContributed.
     *
     * <pre>
     * PV=5 000, extra=2 000 → saldo ajustado = 7 000
     * taxa=1% a.m., 12 meses, PMT=500
     *
     * FV = 7 000×(1.01)^12 + 500×((1.01)^12−1)/0.01
     *    = 7 887,78 + 6 341,25
     *    ≈ 14 229,03
     *
     * totalContributed = 500×12 + 2 000 = 8 000
     * </pre>
     */
    @Test
    void shouldAddExtraContributionToBalanceBeforeCalculation() {
        SimulationInterval interval = SimulationInterval.builder()
                .monthlyContribution(new BigDecimal("500"))
                .periodInMonths(12)
                .extraContribution(new BigDecimal("2000"))
                .build();

        IntervalResult result = strategy.calculate(
                new BigDecimal("5000"), interval, new BigDecimal("0.01"));

        // FV ≈ 14 229,03
        assertThat(result.getFinalBalance().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("14229.03"));

        // totalContributed = PMT×n + extra = 6 000 + 2 000 = 8 000
        assertThat(result.getTotalContributed())
                .isEqualByComparingTo(new BigDecimal("8000"));
    }

    /**
     * Confirma que o FV da ExtraContributionStrategy é maior do que o da
     * SimpleIntervalStrategy com o mesmo saldo inicial (sem o extra), pois
     * o extra rende juros por todo o período.
     */
    @Test
    void shouldProduceLargerBalanceThanSimpleStrategyWithoutExtra() {
        SimulationInterval withExtra = SimulationInterval.builder()
                .monthlyContribution(new BigDecimal("500"))
                .periodInMonths(12)
                .extraContribution(new BigDecimal("1000"))
                .build();

        SimulationInterval withoutExtra = SimulationInterval.builder()
                .monthlyContribution(new BigDecimal("500"))
                .periodInMonths(12)
                .extraContribution(null)
                .build();

        BigDecimal initialBalance = new BigDecimal("5000");
        BigDecimal rate = new BigDecimal("0.01");

        IntervalResult withExtraResult = strategy.calculate(initialBalance, withExtra, rate);
        IntervalResult withoutExtraResult = new SimpleIntervalStrategy()
                .calculate(initialBalance, withoutExtra, rate);

        assertThat(withExtraResult.getFinalBalance())
                .isGreaterThan(withoutExtraResult.getFinalBalance());
    }
}
