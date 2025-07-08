package com.danifgx.kafkapoc.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Very simple test to verify that Kafka mocking works without Spring Boot.
 */
public class VerySimpleKafkaTest {

    @Test
    void kafkaTemplateMockShouldWork() {
        // Create a mock KafkaTemplate
        KafkaTemplate<String, String> mockTemplate = Mockito.mock(KafkaTemplate.class);
        
        // Configure the mock to return a CompletableFuture<SendResult> when send is called
        Mockito.when(mockTemplate.send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));
        
        // Verify that the mock works
        assertNotNull(mockTemplate);
        System.out.println("[DEBUG_LOG] KafkaTemplate mock: " + mockTemplate);
        
        // Verify that the mock returns a CompletableFuture when send is called
        CompletableFuture<SendResult<String, String>> future = mockTemplate.send("topic", "key", "value");
        assertNotNull(future);
        System.out.println("[DEBUG_LOG] CompletableFuture: " + future);
    }
}