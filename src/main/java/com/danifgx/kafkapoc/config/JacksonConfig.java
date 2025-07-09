package com.danifgx.kafkapoc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

/**
 * Configuration for Jackson ObjectMapper to handle serialization/deserialization.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RecordMessageConverter stringJsonMessageConverter(ObjectMapper objectMapper) {
        // Use StringJsonMessageConverter instead of JsonMessageConverter
        // This will handle both JSON objects and strings
        return new StringJsonMessageConverter(objectMapper);
    }
}
