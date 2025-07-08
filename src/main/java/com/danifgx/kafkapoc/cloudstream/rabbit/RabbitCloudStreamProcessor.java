package com.danifgx.kafkapoc.cloudstream.rabbit;

import com.danifgx.kafkapoc.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spring Cloud Stream processor for RabbitMQ.
 * This demonstrates how to use Spring Cloud Stream with RabbitMQ.
 * It processes messages from rabbit-input-queue and sends them to rabbit-output-queue.
 * The implementation is almost identical to the Kafka version, showing the power of Spring Cloud Stream's abstraction.
 */
@Slf4j
@Configuration
public class RabbitCloudStreamProcessor {

    /**
     * Defines a processor function that takes a Message as input and returns a processed Message.
     * The binding is configured in application.properties:
     * - Input: processRabbit-in-0 -> rabbit-input-queue
     * - Output: processRabbit-out-0 -> rabbit-output-queue
     */
    @Bean
    public Function<Message, Message> processRabbit() {
        return message -> {
            log.info("Processing message with Spring Cloud Stream (RabbitMQ): {}", message);
            
            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content("Processed by Spring Cloud Stream (RabbitMQ): " + message.getContent())
                    .sender("Spring Cloud Stream RabbitMQ Processor")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.PROCESSED)
                    .build();
        };
    }
}