package com.danifgx.kafkapoc.service;

import com.danifgx.kafkapoc.kafka.KafkaProducerService;
import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for sending messages to different messaging systems.
 * This service centralizes all message sending functionality to avoid code duplication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final KafkaProducerService kafkaProducerService;
    private final StreamBridge streamBridge;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Send a message using Spring Kafka.
     *
     * @param content The content of the message
     * @param sender The sender of the message
     * @return The sent message
     */
    public Message sendKafkaMessage(String content, String sender) {
        Message message = kafkaProducerService.sendMessage(content);
        log.info("Sent message using Spring Kafka: {}", message);
        return message;
    }

    /**
     * Send a message to Kafka Streams.
     *
     * @param content The content of the message
     * @param sender The sender of the message
     * @return The sent message
     * @throws JsonProcessingException if there's an error processing the message
     */
    public Message sendKafkaStreamsMessage(String content, String sender) throws JsonProcessingException {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(sender)
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        String messageJson = objectMapper.writeValueAsString(message);
        kafkaTemplate.send("stream-input", message.getId(), messageJson);
        log.info("Sent message to Kafka Streams: {}", message);
        
        return message;
    }

    /**
     * Send a message using Spring Cloud Stream with Kafka.
     *
     * @param content The content of the message
     * @param sender The sender of the message
     * @return The sent message
     */
    public Message sendCloudStreamKafkaMessage(String content, String sender) {
        try {
            Message message = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .sender(sender)
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.SIMPLE)
                    .build();

            boolean sent = streamBridge.send("process-in-0", message);
            if (sent) {
                log.info("Sent message to Spring Cloud Stream (Kafka): {}", message);
            } else {
                log.warn("Failed to send message to Spring Cloud Stream (Kafka): {}", message);
            }
            return message;
        } catch (Exception e) {
            log.error("Error sending message to Spring Cloud Stream (Kafka): {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send a message using Spring Cloud Stream with RabbitMQ.
     *
     * @param content The content of the message
     * @param sender The sender of the message
     * @return The sent message
     */
    public Message sendCloudStreamRabbitMessage(String content, String sender) {
        try {
            Message message = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .sender(sender)
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.SIMPLE)
                    .build();

            boolean sent = streamBridge.send("processRabbit-in-0", message);
            if (sent) {
                log.info("Sent message to Spring Cloud Stream (RabbitMQ): {}", message);
            } else {
                log.warn("Failed to send message to Spring Cloud Stream (RabbitMQ): {}", message);
            }
            return message;
        } catch (Exception e) {
            log.error("Error sending message to Spring Cloud Stream (RabbitMQ): {}", e.getMessage(), e);
            throw e;
        }
    }
}