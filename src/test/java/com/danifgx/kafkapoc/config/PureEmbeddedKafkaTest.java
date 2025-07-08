package com.danifgx.kafkapoc.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure EmbeddedKafka test that doesn't load any Spring Boot application context.
 * This test demonstrates the use of EmbeddedKafka in complete isolation from any 
 * external Kafka instance (Docker or otherwise).
 * 
 * This is the proper way to test Kafka functionality without dependencies on external infrastructure.
 */
public class PureEmbeddedKafkaTest {

    private EmbeddedKafkaBroker embeddedKafka;
    
    @BeforeEach
    void setUp() {
        // Start embedded Kafka broker with a test topic
        embeddedKafka = new EmbeddedKafkaKraftBroker(1, 1, "pure-test-topic");
        embeddedKafka.afterPropertiesSet();
    }
    
    @AfterEach
    void tearDown() {
        if (embeddedKafka != null) {
            embeddedKafka.destroy();
        }
    }

    @Test
    void shouldStartEmbeddedKafkaOnRandomPort() {
        assertNotNull(embeddedKafka, "EmbeddedKafka should be initialized");
        
        String brokerAddresses = embeddedKafka.getBrokersAsString();
        assertNotNull(brokerAddresses, "Broker addresses should not be null");
        assertTrue(brokerAddresses.contains("localhost"), "Should run on localhost");
        
        // Verify it's NOT using standard Docker or default ports
        assertFalse(brokerAddresses.contains(":9092"), "Should not use default port 9092");
        assertFalse(brokerAddresses.contains(":29092"), "Should not use Docker port 29092");
        
        System.out.println("✅ Embedded Kafka broker running on: " + brokerAddresses);
        System.out.println("   This proves the test uses EMBEDDED Kafka, not external Docker!");
    }

    @Test
    void shouldProduceAndConsumeMessages() throws Exception {
        String topic = "pure-test-topic";
        String testMessage = "Hello from Pure Embedded Kafka!";
        String testKey = "test-key";
        
        // Configure producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Configure consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Send message
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, testKey, testMessage);
            producer.send(record).get(); // Wait for send to complete
            System.out.println("✅ Message sent to embedded Kafka: " + testMessage);
        }
        
        // Consume message
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            
            // Poll for messages (with timeout)
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            
            assertFalse(records.isEmpty(), "Should receive at least one message");
            
            ConsumerRecord<String, String> receivedRecord = records.iterator().next();
            assertEquals(testKey, receivedRecord.key(), "Key should match");
            assertEquals(testMessage, receivedRecord.value(), "Message content should match");
            assertEquals(topic, receivedRecord.topic(), "Topic should match");
            
            System.out.println("✅ Message received from embedded Kafka: " + receivedRecord.value());
            System.out.println("   This demonstrates complete Kafka functionality using ONLY embedded broker!");
        }
    }
}