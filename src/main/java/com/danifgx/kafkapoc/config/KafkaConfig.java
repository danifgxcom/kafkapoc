package com.danifgx.kafkapoc.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Kafka components.
 */
@Configuration
public class KafkaConfig {

    // Constants for error handling
    private static final int NO_RETRY_INTERVAL = 0;
    private static final int NO_RETRY_ATTEMPTS = 0;

    // Constants for Kafka admin client timeouts (in milliseconds)
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 5000;
    private static final int DEFAULT_API_TIMEOUT_MS = 5000;

    /**
     * Creates a custom error handler for Kafka listeners.
     * This error handler will log errors but continue processing messages.
     *
     * @return The custom error handler
     */
    @Bean
    public CommonErrorHandler errorHandler() {
        // Create a FixedBackOff with no retries
        // This will log the error but continue processing messages
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(NO_RETRY_INTERVAL, NO_RETRY_ATTEMPTS));
        // Skip deserialization exceptions
        errorHandler.addNotRetryableExceptions(org.springframework.kafka.support.converter.ConversionException.class);
        errorHandler.addNotRetryableExceptions(com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
        return errorHandler;
    }

    /**
     * Creates a Kafka listener container factory that uses our custom error handler.
     *
     * @param consumerFactory The consumer factory
     * @param kafkaTemplate The Kafka template
     * @return The Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setReplyTemplate(kafkaTemplate);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Creates a KafkaAdmin bean that can be used to create topics and check Kafka connectivity.
     *
     * @param bootstrapServers The Kafka bootstrap servers
     * @return The KafkaAdmin bean
     */
    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, DEFAULT_REQUEST_TIMEOUT_MS);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, DEFAULT_API_TIMEOUT_MS);
        return new KafkaAdmin(configs);
    }
}
