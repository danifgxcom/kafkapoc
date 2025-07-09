package com.danifgx.kafkapoc.kafka;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        kafkaConsumerService = new KafkaConsumerService(objectMapper);
    }

    @Test
    void consumeMessage_shouldDeserializeAndProcessMessage() throws JsonProcessingException {
        // Given
        String messageJson = "{\"id\":\"test-id\",\"content\":\"Test message\",\"sender\":\"test-sender\",\"timestamp\":\"2023-01-01T12:00:00\",\"type\":\"SIMPLE\"}";
        Message message = Message.builder()
                .id("test-id")
                .content("Test message")
                .sender("test-sender")
                .timestamp(LocalDateTime.of(2023, 1, 1, 12, 0, 0))
                .type(Message.MessageType.SIMPLE)
                .build();
        
        JsonNode jsonNode = mock(JsonNode.class);
        when(jsonNode.isObject()).thenReturn(true);
        when(objectMapper.readTree(messageJson)).thenReturn(jsonNode);
        when(objectMapper.readValue(messageJson, Message.class)).thenReturn(message);

        // Create a spy of the service
        KafkaConsumerService spy = spy(kafkaConsumerService);

        // When
        spy.consumeMessage(messageJson);

        // Then
        verify(objectMapper).readTree(messageJson);
        verify(objectMapper).readValue(messageJson, Message.class);
    }

    @Test
    void consumeMessage_shouldHandleNonJsonMessage() throws JsonProcessingException {
        // Given
        String messageText = "This is not JSON";
        JsonProcessingException exception = new JsonProcessingException("Parsing error") {};
        
        when(objectMapper.readTree(messageText)).thenThrow(exception);

        // When
        kafkaConsumerService.consumeMessage(messageText);

        // Then
        verify(objectMapper).readTree(messageText);
        // No exception should be thrown, just logged
    }

    @Test
    void consumeMessage_shouldHandleNonObjectJson() throws JsonProcessingException {
        // Given
        String messageJson = "\"This is a JSON string but not an object\"";
        JsonNode jsonNode = mock(JsonNode.class);
        when(jsonNode.isObject()).thenReturn(false);
        when(objectMapper.readTree(messageJson)).thenReturn(jsonNode);

        // When
        kafkaConsumerService.consumeMessage(messageJson);

        // Then
        verify(objectMapper).readTree(messageJson);
        // No exception should be thrown, just logged
    }
}