package com.danifgx.kafkapoc.kafka;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        kafkaProducerService = new KafkaProducerService(kafkaTemplate, objectMapper);
    }

    @Test
    void sendMessage_shouldCreateAndSendMessage() throws JsonProcessingException {
        // Given
        String content = "Test message";
        String messageJson = "{\"id\":\"test-id\",\"content\":\"Test message\"}";

        when(objectMapper.writeValueAsString(any(Message.class))).thenReturn(messageJson);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // When
        Message result = kafkaProducerService.sendMessage(content);

        // Then
        assertNotNull(result);
        assertEquals(content, result.getContent());
        assertEquals("Spring Kafka Producer", result.getSender());
        assertEquals(Message.MessageType.SIMPLE, result.getType());
        assertNotNull(result.getId());
        assertNotNull(result.getTimestamp());

        // Verify ObjectMapper was called to serialize the message
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(objectMapper).writeValueAsString(messageCaptor.capture());
        Message capturedMessage = messageCaptor.getValue();
        assertEquals(content, capturedMessage.getContent());

        // Verify KafkaTemplate was called to send the message
        verify(kafkaTemplate).send("simple-messages", result.getId(), messageJson);
    }

    @Test
    void sendMessage_shouldThrowRuntimeException_whenSerializationFails() throws JsonProcessingException {
        // Given
        String content = "Test message";
        JsonProcessingException exception = new JsonProcessingException("Serialization error") {};

        when(objectMapper.writeValueAsString(any(Message.class))).thenThrow(exception);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            kafkaProducerService.sendMessage(content);
        });

        assertEquals("Error sending message to Kafka", thrown.getMessage());
        assertEquals(exception, thrown.getCause());

        // Verify KafkaTemplate was not called
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
