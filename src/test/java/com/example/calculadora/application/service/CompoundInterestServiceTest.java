package com.example.calculadora.application.service;

import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.Simulation;
import com.example.calculadora.domain.model.SimulationInterval;
import com.example.calculadora.domain.model.SimulationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompoundInterestServiceTest {

    @Mock
    private SaveSimulationPort saveSimulationPort;

    @InjectMocks
    private CompoundInterestService service;

    /**
     * Dois intervalos com taxa 12% a.a. (1% a.m.):
     * <ul>
     *   <li>Intervalo 1: PMT=500, 12 meses → totalContributed=6 000</li>
     *   <li>Intervalo 2: PMT=1 000, 24 meses → totalContributed=24 000</li>
     * </ul>
     * totalInvested = 10 000 + 6 000 + 24 000 = 40 000
     */
    @Test
    void shouldCalculateTotalInvestedAndFinalValueForTwoIntervals() {
        BigDecimal initialValue = new BigDecimal("10000");
        BigDecimal annualRate = new BigDecimal("12");

        List<SimulationInterval> intervals = List.of(
                SimulationInterval.builder()
                        .monthlyContribution(new BigDecimal("500"))
                        .periodInMonths(12)
                        .extraContribution(null)
                        .build(),
                SimulationInterval.builder()
                        .monthlyContribution(new BigDecimal("1000"))
                        .periodInMonths(24)
                        .extraContribution(null)
                        .build()
        );

        SimulationResult result = service.calculate(initialValue, annualRate, intervals, "user-test");

        // totalInvested = 10 000 + 6 000 + 24 000 = 40 000
        assertThat(result.getTotalInvested())
                .isEqualByComparingTo(new BigDecimal("40000"));

        // O juro acumulado deve ser positivo
        assertThat(result.getTotalInterest()).isPositive();

        // Dois intervalos no resultado
        assertThat(result.getIntervals()).hasSize(2);

        // O saldo final = totalInvested + totalInterest
        assertThat(result.getFinalValue().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(
                        result.getTotalInvested()
                              .add(result.getTotalInterest())
                              .setScale(2, RoundingMode.HALF_UP));

        // O repositório deve ser chamado exatamente uma vez
        verify(saveSimulationPort, times(1)).save(any(Simulation.class));
    }

    /**
     * Garante que a estratégia ZeroContribution é usada quando PMT=0
     * e que totalContributed do intervalo é 0.
     */
    @Test
    void shouldUseZeroContributionStrategyWhenMonthlyContributionIsZero() {
        List<SimulationInterval> intervals = List.of(
                SimulationInterval.builder()
                        .monthlyContribution(BigDecimal.ZERO)
                        .periodInMonths(6)
                        .extraContribution(null)
                        .build()
        );

        SimulationResult result = service.calculate(
                new BigDecimal("5000"), new BigDecimal("12"), intervals, "user-test");

        assertThat(result.getIntervals().get(0).getTotalContributed())
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Saldo deve crescer apenas com juros
        assertThat(result.getFinalValue()).isGreaterThan(new BigDecimal("5000"));
    }

    /**
     * Garante que ExtraContributionStrategy é resolvida quando o intervalo
     * possui extraContribution > 0.
     */
    @Test
    void shouldUseExtraContributionStrategyWhenExtraIsPositive() {
        List<SimulationInterval> intervals = List.of(
                SimulationInterval.builder()
                        .monthlyContribution(new BigDecimal("500"))
                        .periodInMonths(12)
                        .extraContribution(new BigDecimal("10000"))
                        .build()
        );

        SimulationResult result = service.calculate(
                new BigDecimal("1000"), new BigDecimal("12"), intervals, "user-test");

        // totalContributed do intervalo = PMT×n + extra = 6 000 + 10 000 = 16 000
        assertThat(result.getIntervals().get(0).getTotalContributed())
                .isEqualByComparingTo(new BigDecimal("16000"));
    }
}
