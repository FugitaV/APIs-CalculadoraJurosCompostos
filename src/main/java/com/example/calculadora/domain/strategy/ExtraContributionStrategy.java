package com.example.calculadora.domain.strategy;

import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.SimulationInterval;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia com aporte extra: soma {@code extraContribution} ao saldo antes
 * de delegar o cálculo ao {@link SimpleIntervalStrategy}.
 * O valor extra é incluído em {@code totalContributed}.
 */
public class ExtraContributionStrategy implements IntervalStrategy {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final SimpleIntervalStrategy delegate = new SimpleIntervalStrategy();

    @Override
    public IntervalResult calculate(BigDecimal currentBalance,
                                    SimulationInterval interval,
                                    BigDecimal monthlyRate) {

        BigDecimal extra = interval.getExtraContribution();

        // Injeta o aporte extra no saldo antes do período começar
        BigDecimal adjustedBalance = currentBalance.add(extra);

        IntervalResult base = delegate.calculate(adjustedBalance, interval, monthlyRate);

        // totalContributed = aportes mensais + aporte extra
        BigDecimal totalContributed = base.getTotalContributed().add(extra);

        // juros = saldo final − saldo ajustado − aportes mensais
        BigDecimal interestEarned = base.getFinalBalance()
                .subtract(adjustedBalance)
                .subtract(base.getTotalContributed())
                .setScale(SCALE, ROUNDING);

        return IntervalResult.builder()
                .finalBalance(base.getFinalBalance())
                .totalContributed(totalContributed)
                .interestEarned(interestEarned)
                .build();
    }
}
