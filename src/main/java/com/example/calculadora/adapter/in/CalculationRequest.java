package com.example.calculadora.adapter.in;

import com.example.calculadora.domain.model.SimulationInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Payload de entrada da Lambda (body do API Gateway). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequest {

    BigDecimal initialValue;
    BigDecimal annualRate;
    List<SimulationInterval> intervals;
}
