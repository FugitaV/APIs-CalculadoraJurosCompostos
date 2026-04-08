package com.example.calculadora.adapter.in;

import com.example.calculadora.domain.model.IntervalResult;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/** Payload de resposta serializado pela Lambda. */
@Value
@Builder
public class CalculationResponse {

    String simulationId;

    BigDecimal totalInvested;
    BigDecimal totalInterest;
    BigDecimal finalValue;

    /** Detalhamento de cada intervalo. */
    List<IntervalResult> intervals;
}
