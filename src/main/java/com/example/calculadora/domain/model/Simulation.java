package com.example.calculadora.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entidade DynamoDB para persistência de simulações.
 *
 * Tabela : simulations
 * PK     : simulationId  (String, UUID)
 * Demais : userId, createdAt, ttl, initialValue, annualRate,
 *          intervals[], totalInvested, totalInterest, finalValue, intervalResults[]
 */
@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Simulation {

    private String simulationId;
    private String userId;
    private String createdAt;
    private Long ttl;
    private BigDecimal initialValue;
    private BigDecimal annualRate;
    private List<SimulationInterval> intervals;
    private BigDecimal totalInvested;
    private BigDecimal totalInterest;
    private BigDecimal finalValue;
    private List<IntervalResult> intervalResults;

    @DynamoDbPartitionKey
    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }

    public BigDecimal getInitialValue() { return initialValue; }
    public void setInitialValue(BigDecimal initialValue) { this.initialValue = initialValue; }

    public BigDecimal getAnnualRate() { return annualRate; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }

    public List<SimulationInterval> getIntervals() { return intervals; }
    public void setIntervals(List<SimulationInterval> intervals) { this.intervals = intervals; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public BigDecimal getFinalValue() { return finalValue; }
    public void setFinalValue(BigDecimal finalValue) { this.finalValue = finalValue; }

    public List<IntervalResult> getIntervalResults() { return intervalResults; }
    public void setIntervalResults(List<IntervalResult> intervalResults) { this.intervalResults = intervalResults; }
}
