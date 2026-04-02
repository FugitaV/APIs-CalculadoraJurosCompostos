package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia padrão: aporte mensal fixo com juros compostos.
 *
 * <pre>
 * FV = PV × (1+r)^n  +  PMT × ((1+r)^n − 1) / r
 * </pre>
 *
 * Toda aritmética usa {@link RoundingMode#HALF_UP} com escala 10.
 */
public class SimpleIntervalStrategy implements IntervalStrategy {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Override
    public IntervalResult calculate(BigDecimal currentBalance,
                                    SimulationInterval interval,
                                    BigDecimal monthlyRate) {

        int n = interval.getPeriodInMonths();
        BigDecimal pmt = interval.getMonthlyContribution();
        BigDecimal r = monthlyRate;

        // (1 + r)^n
        BigDecimal onePlusRPowN = BigDecimal.ONE.add(r).pow(n);

        // PV × (1+r)^n
        BigDecimal fvPrincipal = currentBalance
                .multiply(onePlusRPowN)
                .setScale(SCALE, ROUNDING);

        // PMT × ((1+r)^n − 1) / r
        BigDecimal factor = onePlusRPowN
                .subtract(BigDecimal.ONE)
                .divide(r, SCALE, ROUNDING);
        BigDecimal fvContributions = pmt.multiply(factor).setScale(SCALE, ROUNDING);

        BigDecimal finalBalance = fvPrincipal.add(fvContributions);

        BigDecimal totalContributed = pmt.multiply(BigDecimal.valueOf(n));
        BigDecimal interestEarned = finalBalance
                .subtract(currentBalance)
                .subtract(totalContributed)
                .setScale(SCALE, ROUNDING);

        return IntervalResult.builder()
                .finalBalance(finalBalance)
                .totalContributed(totalContributed)
                .interestEarned(interestEarned)
                .build();
    }
}
