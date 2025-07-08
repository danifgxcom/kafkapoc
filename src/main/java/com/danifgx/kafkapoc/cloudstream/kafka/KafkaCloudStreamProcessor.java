package com.danifgx.kafkapoc.cloudstream.kafka;

import com.danifgx.kafkapoc.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spring Cloud Stream processor for Kafka.
 * This demonstrates how to use Spring Cloud Stream with Kafka.
 * It processes messages from cloud-stream-topic and sends them to cloud-stream-topic-processed.
 */
@Slf4j
@Configuration
public class KafkaCloudStreamProcessor {

    /**
     * Defines a processor function that takes a Message as input and returns a processed Message.
     * The binding is configured in application.properties:
     * - Input: process-in-0 -> cloud-stream-topic
     * - Output: process-out-0 -> cloud-stream-topic-processed
     */
    @Bean
    public Function<Message, Message> process() {
        return message -> {
            log.info("Processing message with Spring Cloud Stream (Kafka): {}", message);
            
            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content("Processed by Spring Cloud Stream (Kafka): " + message.getContent())
                    .sender("Spring Cloud Stream Processor")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.PROCESSED)
                    .build();
        };
    }
}