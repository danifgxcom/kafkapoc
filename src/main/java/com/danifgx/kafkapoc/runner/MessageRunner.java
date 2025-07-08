package com.danifgx.kafkapoc.runner;

import com.danifgx.kafkapoc.model.Message;
import com.danifgx.kafkapoc.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CommandLineRunner that sends messages to different messaging systems when the application starts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class MessageRunner implements CommandLineRunner {

    private final MessageService messageService;
    private final KafkaAdmin kafkaAdmin;
    private final ConnectionFactory rabbitConnectionFactory;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting to send messages to different messaging systems...");

        try {
            // Check if Kafka is available
            boolean kafkaAvailable = isKafkaAvailable();
            // Check if RabbitMQ is available
            boolean rabbitAvailable = isRabbitMQAvailable();

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

            if (rabbitAvailable) {
                log.info("RabbitMQ is available. Sending messages...");
                // Send message using Spring Cloud Stream with RabbitMQ
                sendCloudStreamRabbitMessage("Hello from Spring Cloud Stream (RabbitMQ)!");
            } else {
                log.warn("RabbitMQ is not available. Skipping RabbitMQ messages.");
            }

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
        Message message = messageService.sendKafkaMessage(content, "Message Runner");
        log.info("Sent message using Spring Kafka: {}", message);
    }

    /**
     * Send a message to Kafka Streams.
     *
     * @param content The content of the message
     */
    private void sendKafkaStreamsMessage(String content) throws JsonProcessingException {
        Message message = messageService.sendKafkaStreamsMessage(content, "Message Runner");
        log.info("Sent message to Kafka Streams: {}", message);
    }

    /**
     * Send a message using Spring Cloud Stream with Kafka.
     *
     * @param content The content of the message
     */
    private void sendCloudStreamKafkaMessage(String content) {
        try {
            Message message = messageService.sendCloudStreamKafkaMessage(content, "Message Runner");
            log.info("Sent message to Spring Cloud Stream (Kafka): {}", message);
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
            Message message = messageService.sendCloudStreamRabbitMessage(content, "Message Runner");
            log.info("Sent message to Spring Cloud Stream (RabbitMQ): {}", message);
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

    /**
     * Checks if RabbitMQ is available by trying to create a connection.
     * 
     * @return true if RabbitMQ is available, false otherwise
     */
    private boolean isRabbitMQAvailable() {
        try (Connection connection = rabbitConnectionFactory.createConnection()) {
            boolean isConnected = connection.isOpen();
            if (isConnected) {
                log.info("Successfully connected to RabbitMQ");
            }
            return isConnected;
        } catch (Exception e) {
            log.warn("RabbitMQ is not available: {}", e.getMessage());
            return false;
        }
    }
}
