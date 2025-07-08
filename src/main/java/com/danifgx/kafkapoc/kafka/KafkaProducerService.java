package com.danifgx.kafkapoc.kafka;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for producing messages to Kafka using Spring Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sends a message to the simple-messages topic.
     *
     * @param content The content of the message
     * @return The sent message
     */
    public Message sendMessage(String content) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("Spring Kafka Producer")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("simple-messages", message.getId(), messageJson);
            log.info("Sent message to Kafka: {}", message);
            return message;
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending message to Kafka", e);
        }
    }
}