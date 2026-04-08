package com.example.calculadora.application.service;

import com.example.calculadora.application.port.CalculateInterestUseCase;
import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.IntervalResult;
import com.example.calculadora.domain.model.Simulation;
import com.example.calculadora.domain.model.SimulationInterval;
import com.example.calculadora.domain.model.SimulationResult;
import com.example.calculadora.domain.strategy.ExtraContributionStrategy;
import com.example.calculadora.domain.strategy.IntervalStrategy;
import com.example.calculadora.domain.strategy.SimpleIntervalStrategy;
import com.example.calculadora.domain.strategy.ZeroContributionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompoundInterestService implements CalculateInterestUseCase {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final SaveSimulationPort saveSimulationPort;

    @Override
    public SimulationResult calculate(BigDecimal initialValue,
                                      BigDecimal annualRate,
                                      List<SimulationInterval> intervals,
                                      String userId) {

        // Converte taxa anual efetiva em taxa mensal equivalente (conversão composta):
        // r_mensal = (1 + r_anual) ^ (1/12) - 1
        // Ex: 12% a.a. → (1,12)^(1/12) - 1 ≈ 0,9489% a.m.
        double annualRateDecimal = annualRate.doubleValue() / 100.0;
        double monthlyRateDouble = Math.pow(1.0 + annualRateDecimal, 1.0 / 12.0) - 1.0;
        BigDecimal monthlyRate = BigDecimal.valueOf(monthlyRateDouble).setScale(SCALE, ROUNDING);

        BigDecimal currentBalance = initialValue;
        List<IntervalResult> intervalResults = new ArrayList<>();

        for (SimulationInterval interval : intervals) {
            IntervalStrategy strategy = resolveStrategy(interval);
            IntervalResult result = strategy.calculate(currentBalance, interval, monthlyRate);
            intervalResults.add(result);
            currentBalance = result.getFinalBalance();
        }

        BigDecimal totalContributions = intervalResults.stream()
                .map(IntervalResult::getTotalContributed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvested = initialValue.add(totalContributions);
        BigDecimal finalValue = currentBalance;
        BigDecimal totalInterest = finalValue.subtract(totalInvested).setScale(SCALE, ROUNDING);

        SimulationResult partialResult = SimulationResult.builder()
                .totalInvested(totalInvested)
                .totalInterest(totalInterest)
                .finalValue(finalValue)
                .intervals(intervalResults)
                .build();

        String simulationId = persistSimulation(userId, initialValue, annualRate, intervals, partialResult);

        return SimulationResult.builder()
                .simulationId(simulationId)
                .totalInvested(totalInvested)
                .totalInterest(totalInterest)
                .finalValue(finalValue)
                .intervals(intervalResults)
                .build();
    }

    // ── resolução de estratégia ────────────────────────────────────────────────

    private IntervalStrategy resolveStrategy(SimulationInterval interval) {
        if (interval.getExtraContribution() != null
                && interval.getExtraContribution().compareTo(BigDecimal.ZERO) > 0) {
            return new ExtraContributionStrategy();
        }
        if (interval.getMonthlyContribution().compareTo(BigDecimal.ZERO) > 0) {
            return new SimpleIntervalStrategy();
        }
        return new ZeroContributionStrategy();
    }

    // ── persistência ──────────────────────────────────────────────────────────

    private String persistSimulation(String userId,
                                     BigDecimal initialValue,
                                     BigDecimal annualRate,
                                     List<SimulationInterval> intervals,
                                     SimulationResult result) {
        String simulationId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        long ttl = Instant.now().plus(90, ChronoUnit.DAYS).getEpochSecond();

        Simulation simulation = Simulation.builder()
                .simulationId(simulationId)
                .userId(userId)
                .createdAt(createdAt)
                .ttl(ttl)
                .initialValue(initialValue)
                .annualRate(annualRate)
                .intervals(intervals)
                .totalInvested(result.getTotalInvested())
                .totalInterest(result.getTotalInterest())
                .finalValue(result.getFinalValue())
                .intervalResults(result.getIntervals())
                .build();

        saveSimulationPort.save(simulation);
        return simulationId;
    }
}
