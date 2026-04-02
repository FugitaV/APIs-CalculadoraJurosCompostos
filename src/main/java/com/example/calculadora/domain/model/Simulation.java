package com.example.calculadora.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;

/**
 * Entidade DynamoDB para persistência de simulações.
 *
 * Tabela : simulations
 * PK     : userId#simulationId  (String)
 * SK     : createdAt            (String ISO-8601)
 * TTL    : ttl                  (Long, Unix epoch, expira em 90 dias)
 * GSI    : userId-createdAt-index  (userId → SK)
 */
@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Simulation {

    // ── campos com anotações DynamoDB em getters explícitos ──────────────────

    private String pk;
    private String sk;
    private String userId;

    // ── campos restantes com getters/setters manuais ─────────────────────────

    private String simulationId;
    private Long ttl;
    private BigDecimal initialValue;
    private BigDecimal annualRate;
    private BigDecimal totalInvested;
    private BigDecimal totalInterest;
    private BigDecimal finalValue;

    // ── PK ───────────────────────────────────────────────────────────────────

    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    // ── SK — serve também como sort key do GSI ────────────────────────────────

    @DynamoDbSortKey
    @DynamoDbSecondarySortKey(indexNames = {"userId-createdAt-index"})
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    // ── GSI partition key ─────────────────────────────────────────────────────

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId-createdAt-index"})
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // ── demais campos ─────────────────────────────────────────────────────────

    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }

    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }

    public BigDecimal getInitialValue() { return initialValue; }
    public void setInitialValue(BigDecimal initialValue) { this.initialValue = initialValue; }

    public BigDecimal getAnnualRate() { return annualRate; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public BigDecimal getFinalValue() { return finalValue; }
    public void setFinalValue(BigDecimal finalValue) { this.finalValue = finalValue; }
}
