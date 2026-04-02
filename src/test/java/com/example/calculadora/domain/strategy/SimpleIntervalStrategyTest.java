package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleIntervalStrategyTest {

    private final SimpleIntervalStrategy strategy = new SimpleIntervalStrategy();

    /**
     * Valida FV para: PV=0, taxa=1% a.m., 12 meses, PMT=500.
     *
     * <pre>
     * FV = 500 × ((1.01)^12 − 1) / 0.01
     *    = 500 × 12.6825030131...
     *    ≈ 6 341,25
     * </pre>
     */
    @Test
    void shouldCalculateFinalValueWith1PercentMonthlyRateOver12MonthsAndPmt500() {
        SimulationInterval interval = SimulationInterval.builder()
                .monthlyContribution(new BigDecimal("500"))
                .periodInMonths(12)
                .extraContribution(null)
                .build();

        IntervalResult result = strategy.calculate(
                BigDecimal.ZERO, interval, new BigDecimal("0.01"));

        // FV ≈ 6 341,25
        assertThat(result.getFinalBalance().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("6341.25"));

        // totalContributed = 500 × 12 = 6 000
        assertThat(result.getTotalContributed())
                .isEqualByComparingTo(new BigDecimal("6000"));

        // juros = 6 341,25 − 6 000 ≈ 341,25
        assertThat(result.getInterestEarned().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("341.25"));
    }

    /**
     * Com saldo inicial positivo, o montante principal também rende juros.
     *
     * <pre>
     * PV=10 000, taxa=1% a.m., 12 meses, PMT=500
     * FV = 10 000×(1.01)^12 + 500×((1.01)^12−1)/0.01
     *    = 11 268,25 + 6 341,25 = 17 609,50
     * </pre>
     */
    @Test
    void shouldIncludeInitialBalanceInCompounding() {
        SimulationInterval interval = SimulationInterval.builder()
                .monthlyContribution(new BigDecimal("500"))
                .periodInMonths(12)
                .extraContribution(null)
                .build();

        IntervalResult result = strategy.calculate(
                new BigDecimal("10000"), interval, new BigDecimal("0.01"));

        assertThat(result.getFinalBalance().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("17609.50"));

        assertThat(result.getFinalBalance())
                .isGreaterThan(new BigDecimal("17000"));
    }
}
