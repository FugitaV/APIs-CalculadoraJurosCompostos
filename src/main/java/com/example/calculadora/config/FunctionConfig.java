package com.example.calculadora.config;

import com.example.calculadora.adapter.in.CalculationFunction;
import com.example.calculadora.adapter.in.CalculationRequest;
import com.example.calculadora.adapter.in.CalculationResponse;
import com.example.calculadora.application.port.CalculateInterestUseCase;
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
     * Registra a função principal.
     * O nome do bean ("calculationFunction") deve coincidir com
     * {@code spring.cloud.function.definition} no application.yml.
     */
    @Bean
    public Function<Message<CalculationRequest>, CalculationResponse> calculationFunction(
            CalculateInterestUseCase calculateInterestUseCase) {
        return new CalculationFunction(calculateInterestUseCase);
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
