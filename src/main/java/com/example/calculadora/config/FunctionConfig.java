package com.example.calculadora.config;

import com.example.calculadora.adapter.in.CalculationFunction;
import com.example.calculadora.adapter.in.CalculationRequest;
import com.example.calculadora.adapter.in.CalculationResponse;
import com.example.calculadora.adapter.in.ExportFunction;
import com.example.calculadora.adapter.in.ExportRequest;
import com.example.calculadora.adapter.in.ExportResponse;
import com.example.calculadora.application.port.CalculateInterestUseCase;
import com.example.calculadora.application.port.ExportSimulationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.function.Function;

@Configuration
public class FunctionConfig {

    /**
     * Função de cálculo de juros compostos.
     * Ativa quando {@code SPRING_CLOUD_FUNCTION_DEFINITION=calculationFunction} (padrão no application.yml).
     */
    @Bean
    public Function<Message<CalculationRequest>, CalculationResponse> calculationFunction(
            CalculateInterestUseCase calculateInterestUseCase) {
        return new CalculationFunction(calculateInterestUseCase);
    }

    /**
     * Função de exportação de Excel.
     * Ativa quando {@code SPRING_CLOUD_FUNCTION_DEFINITION=exportFunction}
     * (env var da segunda Lambda — mesmo JAR, função diferente).
     */
    @Bean
    public Function<Message<ExportRequest>, ExportResponse> exportFunction(
            ExportSimulationUseCase exportSimulationUseCase) {
        return new ExportFunction(exportSimulationUseCase);
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
