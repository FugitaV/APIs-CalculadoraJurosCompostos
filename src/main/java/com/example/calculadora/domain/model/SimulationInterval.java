package com.example.calculadora.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.math.BigDecimal;

/**
 * Representa um intervalo de investimento com aporte mensal fixo,
 * duração e aporte extra opcional injetado no início do período.
 */
@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationInterval {

    BigDecimal monthlyContribution;
    int periodInMonths;
    BigDecimal extraContribution;
}
