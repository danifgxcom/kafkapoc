package com.danifgx.kafkapoc.kafka;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka using Spring Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;

    /**
     * Listens for messages on the simple-messages topic.
     *
     * @param messageJson The JSON message from Kafka
     */
    @KafkaListener(topics = "simple-messages", groupId = "simple-consumer-group")
    public void consumeMessage(String messageJson) {
        try {
            Message message = parseMessage(messageJson);
            log.info("Received message from Kafka: {}", message);
            processMessage(message);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    private Message parseMessage(String messageJson) {
        try {
            // First try to parse as a JSON object
            JsonNode jsonNode = objectMapper.readTree(messageJson);
            if (jsonNode.isObject()) {
                return objectMapper.readValue(messageJson, Message.class);
            } else {
                // If it's not an object, treat it as a string
                return createStringMessage(messageJson);
            }
        } catch (JsonProcessingException e) {
            // If parsing fails, treat it as a string
            log.debug("Failed to parse as JSON, treating as string: {}", e.getMessage());
            return createStringMessage(messageJson);
        }
    }

    private Message createStringMessage(String content) {
        return Message.builder()
                .id(java.util.UUID.randomUUID().toString())
                .content(content)
                .sender("Unknown")
                .timestamp(java.time.LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();
    }

    /**
     * Process the received message.
     * This is a placeholder for actual message processing logic.
     *
     * @param message The message to process
     */
    private void processMessage(Message message) {
        log.info("Processing message with Spring Kafka Listener: {}", message.getContent());
        // Add your business logic here
    }
}
