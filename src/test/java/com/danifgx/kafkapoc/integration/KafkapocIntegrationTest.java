package com.danifgx.kafkapoc.integration;

import com.danifgx.kafkapoc.config.JacksonConfig;
import com.danifgx.kafkapoc.config.TestKafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple integration test for the Kafkapoc application.
 * 
 * This test verifies that the application context loads correctly.
 * For a real-world application, you would want to create more comprehensive integration tests
 * that verify the end-to-end messaging flows using test containers or embedded brokers.
 */
@SpringBootTest(classes = {TestKafkaConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class KafkapocIntegrationTest {

    @Test
    void contextLoads() {
        // Just verify that the application context loads correctly
        System.out.println("[DEBUG_LOG] Running contextLoads test");
    }
}
