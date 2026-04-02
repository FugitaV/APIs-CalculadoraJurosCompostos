package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia sem aporte mensal (pausa de contribuição).
 *
 * <pre>
 * FV = PV × (1+r)^n
 * </pre>
 */
public class ZeroContributionStrategy implements IntervalStrategy {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Override
    public IntervalResult calculate(BigDecimal currentBalance,
                                    SimulationInterval interval,
                                    BigDecimal monthlyRate) {

        int n = interval.getPeriodInMonths();

        BigDecimal finalBalance = currentBalance
                .multiply(BigDecimal.ONE.add(monthlyRate).pow(n))
                .setScale(SCALE, ROUNDING);

        BigDecimal interestEarned = finalBalance
                .subtract(currentBalance)
                .setScale(SCALE, ROUNDING);

        return IntervalResult.builder()
                .finalBalance(finalBalance)
                .totalContributed(BigDecimal.ZERO)
                .interestEarned(interestEarned)
                .build();
    }
}
