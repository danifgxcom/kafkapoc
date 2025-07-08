package com.danifgx.kafkapoc.cloudstream.rabbit;

import com.danifgx.kafkapoc.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el procesador de RabbitMQ.
 */
class RabbitCloudStreamProcessorTest {

    private RabbitCloudStreamProcessor processor;
    private Function<Message, Message> processFunction;

    @BeforeEach
    void setUp() {
        processor = new RabbitCloudStreamProcessor();
        processFunction = processor.processRabbit();
    }

    @Test
    void processRabbit_shouldTransformMessage() {
        // Given
        String originalContent = "Test message";
        Message inputMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(originalContent)
                .sender("Test Sender")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // When
        Message result = processFunction.apply(inputMessage);

        // Then
        assertNotNull(result);
        assertNotEquals(inputMessage.getId(), result.getId());
        assertEquals("Processed by Spring Cloud Stream (RabbitMQ): " + originalContent, result.getContent());
        assertEquals("Spring Cloud Stream RabbitMQ Processor", result.getSender());
        assertNotNull(result.getTimestamp());
        assertEquals(Message.MessageType.PROCESSED, result.getType());
    }

    @Test
    void processRabbit_shouldHandleNullContent() {
        // Given
        Message inputMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(null)
                .sender("Test Sender")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // When
        Message result = processFunction.apply(inputMessage);

        // Then
        assertNotNull(result);
        assertEquals("Processed by Spring Cloud Stream (RabbitMQ): null", result.getContent());
    }

    @Test
    void processRabbit_shouldHandleEmptyContent() {
        // Given
        Message inputMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("")
                .sender("Test Sender")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // When
        Message result = processFunction.apply(inputMessage);

        // Then
        assertNotNull(result);
        assertEquals("Processed by Spring Cloud Stream (RabbitMQ): ", result.getContent());
    }
}
