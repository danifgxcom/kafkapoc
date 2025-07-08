package com.danifgx.kafkapoc.cloudstream.kafka;

import com.danifgx.kafkapoc.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class KafkaCloudStreamProcessorTest {

    private KafkaCloudStreamProcessor processor;
    private Function<Message, Message> processFunction;

    @BeforeEach
    void setUp() {
        processor = new KafkaCloudStreamProcessor();
        processFunction = processor.process();
    }

    @Test
    void process_shouldTransformMessage() {
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
        assertEquals("Processed by Spring Cloud Stream (Kafka): " + originalContent, result.getContent());
        assertEquals("Spring Cloud Stream Processor", result.getSender());
        assertNotNull(result.getTimestamp());
        assertEquals(Message.MessageType.PROCESSED, result.getType());
    }

    @Test
    void process_shouldHandleNullContent() {
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
        assertEquals("Processed by Spring Cloud Stream (Kafka): null", result.getContent());
    }

    @Test
    void process_shouldHandleEmptyContent() {
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
        assertEquals("Processed by Spring Cloud Stream (Kafka): ", result.getContent());
    }
}