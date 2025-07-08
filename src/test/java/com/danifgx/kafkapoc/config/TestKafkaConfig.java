package com.danifgx.kafkapoc.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Test configuration for Kafka components.
 * Provides mock Kafka beans for testing.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        // Create a mock KafkaTemplate for testing
        KafkaTemplate<String, String> mockTemplate = Mockito.mock(KafkaTemplate.class);

        // Configure the mock to return a CompletableFuture<SendResult> when send is called
        Mockito.when(mockTemplate.send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
               .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));

        return mockTemplate;
    }

    @Bean(name = "defaultKafkaStreamsConfig")
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-streams-app");
        // Use a non-standard port to avoid conflicts with real Kafka
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, org.apache.kafka.common.serialization.Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, org.apache.kafka.common.serialization.Serdes.String().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, "target/kafka-streams-test");

        return new KafkaStreamsConfiguration(props);
    }
}
