package com.example.calculadora.adapter.in;

import com.example.calculadora.domain.model.SimulationInterval;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/** Payload de entrada da Lambda (body do API Gateway). */
@Value
@Builder
@Jacksonized
public class CalculationRequest {

    /** Valor inicial investido. */
    BigDecimal initialValue;

    /** Taxa de juros anual em percentual (ex: 12.00 = 12 % a.a.). */
    BigDecimal annualRate;

    /** Lista de intervalos de investimento, executados sequencialmente. */
    List<SimulationInterval> intervals;
}
