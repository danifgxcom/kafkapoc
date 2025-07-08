package com.danifgx.kafkapoc.runner;

import com.danifgx.kafkapoc.kafka.KafkaProducerService;
import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CommandLineRunner that sends messages to different messaging systems when the application starts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class MessageRunner implements CommandLineRunner {

    private final KafkaProducerService kafkaProducerService;
    private final StreamBridge streamBridge;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaAdmin kafkaAdmin;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting to send messages to different messaging systems...");

        try {
            // Check if Kafka is available
            boolean kafkaAvailable = isKafkaAvailable();

            if (kafkaAvailable) {
                log.info("Kafka is available. Sending messages...");

                // Send message using Spring Kafka
                sendKafkaMessage("Hello from Spring Kafka!");

                // Send message to Kafka Streams
                sendKafkaStreamsMessage("Hello from Kafka Streams!");

                // Send message using Spring Cloud Stream with Kafka
                sendCloudStreamKafkaMessage("Hello from Spring Cloud Stream (Kafka)!");
            } else {
                log.warn("Kafka is not available. Skipping Kafka messages.");
            }

            // Send message using Spring Cloud Stream with RabbitMQ
            sendCloudStreamRabbitMessage("Hello from Spring Cloud Stream (RabbitMQ)!");

            log.info("All messages sent successfully!");
        } catch (Exception e) {
            log.error("Error sending messages: {}. Application will continue running.", e.getMessage());
        }
    }

    /**
     * Send a message using Spring Kafka.
     *
     * @param content The content of the message
     */
    private void sendKafkaMessage(String content) {
        Message message = kafkaProducerService.sendMessage(content);
        log.info("Sent message using Spring Kafka: {}", message);
    }

    /**
     * Send a message to Kafka Streams.
     *
     * @param content The content of the message
     */
    private void sendKafkaStreamsMessage(String content) throws JsonProcessingException {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("Message Runner")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        String messageJson = objectMapper.writeValueAsString(message);
        kafkaTemplate.send("stream-input", message.getId(), messageJson);
        log.info("Sent message to Kafka Streams: {}", message);
    }

    /**
     * Send a message using Spring Cloud Stream with Kafka.
     *
     * @param content The content of the message
     */
    private void sendCloudStreamKafkaMessage(String content) {
        try {
            Message message = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .sender("Message Runner")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.SIMPLE)
                    .build();

            boolean sent = streamBridge.send("process-in-0", message);
            if (sent) {
                log.info("Sent message to Spring Cloud Stream (Kafka): {}", message);
            } else {
                log.warn("Failed to send message to Spring Cloud Stream (Kafka): {}", message);
            }
        } catch (Exception e) {
            log.error("Error sending message to Spring Cloud Stream (Kafka): {}", e.getMessage(), e);
        }
    }

    /**
     * Send a message using Spring Cloud Stream with RabbitMQ.
     *
     * @param content The content of the message
     */
    private void sendCloudStreamRabbitMessage(String content) {
        try {
            Message message = Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .sender("Message Runner")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.SIMPLE)
                    .build();

            boolean sent = streamBridge.send("processRabbit-in-0", message);
            if (sent) {
                log.info("Sent message to Spring Cloud Stream (RabbitMQ): {}", message);
            } else {
                log.warn("Failed to send message to Spring Cloud Stream (RabbitMQ): {}", message);
            }
        } catch (Exception e) {
            log.error("Error sending message to Spring Cloud Stream (RabbitMQ): {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if Kafka is available by trying to describe the cluster.
     * 
     * @return true if Kafka is available, false otherwise
     */
    private boolean isKafkaAvailable() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult describeClusterResult = adminClient.describeCluster();
            // Try to get the cluster ID with a timeout
            describeClusterResult.clusterId().get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka is not available: {}", e.getMessage());
            return false;
        }
    }
}
