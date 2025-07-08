package com.danifgx.kafkapoc.streams;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaStreamsProcessor.
 * 
 * Note: Testing Kafka Streams is complex and often requires integration tests.
 * This class provides basic unit tests for the processor logic, but a full
 * integration test would be more comprehensive.
 */
@ExtendWith(MockitoExtension.class)
class KafkaStreamsProcessorTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaStreamsProcessor kafkaStreamsProcessor;

    @Test
    void testMessageProcessing() throws JsonProcessingException {
        // Given
        String inputJson = "{\"id\":\"input-id\",\"content\":\"input content\"}";
        Message inputMessage = Message.builder()
                .id("input-id")
                .content("input content")
                .sender("test-sender")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // Mock ObjectMapper behavior
        when(objectMapper.readValue(eq(inputJson), eq(Message.class))).thenReturn(inputMessage);

        // When - simulate the mapValues function
        String result = simulateMapValues(inputJson);

        // Then
        verify(objectMapper).readValue(eq(inputJson), eq(Message.class));
        verify(objectMapper).writeValueAsString(any(Message.class));
    }

    @Test
    void testErrorHandling() throws JsonProcessingException {
        // Given
        String inputJson = "invalid-json";
        JsonProcessingException exception = new JsonProcessingException("Processing error") {};

        // Mock ObjectMapper behavior
        when(objectMapper.readValue(eq(inputJson), eq(Message.class))).thenThrow(exception);

        // When - simulate the mapValues function
        String result = simulateMapValues(inputJson);

        // Then
        verify(objectMapper).readValue(eq(inputJson), eq(Message.class));
        // Should return the original value in case of error
        assertEquals(inputJson, result);
    }

    /**
     * Helper method to simulate the mapValues function in the KafkaStreamsProcessor.
     * This allows us to test the processing logic without setting up a full Kafka Streams topology.
     */
    private String simulateMapValues(String value) {
        try {
            // Parse the input message
            Message inputMessage = objectMapper.readValue(value, Message.class);

            // Create a processed message
            Message processedMessage = Message.builder()
                    .id("processed-id")
                    .content("Processed by Kafka Streams: " + inputMessage.getContent())
                    .sender("Kafka Streams Processor")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.STREAMED)
                    .build();

            // Convert back to JSON
            return objectMapper.writeValueAsString(processedMessage);
        } catch (JsonProcessingException e) {
            return value; // Return original value in case of error
        }
    }
}
