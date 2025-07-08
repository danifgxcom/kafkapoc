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
            // Check if the message is a JSON object or a string
            Message message;
            try {
                // First try to parse as a JSON object
                JsonNode jsonNode = objectMapper.readTree(messageJson);
                if (jsonNode.isObject()) {
                    message = objectMapper.readValue(messageJson, Message.class);
                } else {
                    // If it's not an object, treat it as a string
                    message = Message.builder()
                            .content(messageJson)
                            .build();
                }
            } catch (JsonProcessingException e) {
                // If parsing fails, treat it as a string
                message = Message.builder()
                        .content(messageJson)
                        .build();
            }

            log.info("Received message from Kafka: {}", message);

            // In a real application, you would process the message here
            processMessage(message);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
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
