package com.example.calculadora.adapter.out;

import com.example.calculadora.application.port.SaveSimulationPort;
import com.example.calculadora.domain.model.Simulation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("!local")
@RequiredArgsConstructor
public class DynamoSimulationRepository implements SaveSimulationPort {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${DYNAMODB_TABLE_NAME:simulations}")
    private String tableName;

    @Override
    public void save(Simulation simulation) {
        DynamoDbTable<Simulation> table = dynamoDbEnhancedClient
                .table(tableName, TableSchema.fromBean(Simulation.class));
        table.putItem(simulation);
    }
}
