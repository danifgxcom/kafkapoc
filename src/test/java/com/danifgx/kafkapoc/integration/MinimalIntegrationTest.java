package com.danifgx.kafkapoc.integration;

import com.danifgx.kafkapoc.config.JacksonConfig;
import com.danifgx.kafkapoc.config.TestKafkaConfig;
import com.danifgx.kafkapoc.kafka.KafkaProducerService;
import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Minimal integration test for the Kafkapoc application.
 * 
 * This test verifies that the KafkaProducerService works correctly with a mock KafkaTemplate.
 */
@SpringBootTest(classes = {TestKafkaConfig.class, KafkaProducerService.class, JacksonConfig.class})
@ActiveProfiles("test")
class MinimalIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void kafkaProducerServiceShouldSendMessage() {
        System.out.println("[DEBUG_LOG] Running kafkaProducerServiceShouldSendMessage test");

        // Send a message
        Message message = kafkaProducerService.sendMessage("Test message");

        // Verify that the message was sent
        assertNotNull(message);
        System.out.println("[DEBUG_LOG] Message: " + message);

        // Verify that the KafkaTemplate was called
        verify(kafkaTemplate).send(eq("simple-messages"), anyString(), anyString());
    }
}
