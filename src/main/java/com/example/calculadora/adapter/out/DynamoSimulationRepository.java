package com.example.calculadora.adapter.out;

import com.example.calculadora.application.port.GetSimulationPort;
import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.Simulation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DynamoSimulationRepository implements SaveSimulationPort, GetSimulationPort {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${DYNAMODB_TABLE_NAME:simulations}")
    private String tableName;

    @Override
    public void save(Simulation simulation) {
        table().putItem(simulation);
    }

    @Override
    public Optional<Simulation> getById(String simulationId) {
        Key key = Key.builder().partitionValue(simulationId).build();
        return Optional.ofNullable(table().getItem(key));
    }

    private DynamoDbTable<Simulation> table() {
        return dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(Simulation.class));
    }
}
