package com.danifgx.kafkapoc.streams;

import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Streams processor that demonstrates how to use Kafka Streams API.
 * It reads messages from stream-input topic, processes them, and writes to stream-output topic.
 */
@Slf4j
@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
public class KafkaStreamsProcessor {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        // Create JSON Serde for Message class
        final Serde<String> stringSerde = Serdes.String();
        final JsonSerde<Message> messageSerde = new JsonSerde<>(Message.class, objectMapper);

        // Create a stream from the input topic
        KStream<String, String> inputStream = streamsBuilder.stream(
                "stream-input", 
                Consumed.with(stringSerde, stringSerde)
        );

        // Process the stream
        KStream<String, String> processedStream = inputStream.mapValues(value -> {
            try {
                // Parse the input message
                Message inputMessage = objectMapper.readValue(value, Message.class);
                log.info("Processing message with Kafka Streams: {}", inputMessage);
                
                // Create a processed message
                Message processedMessage = Message.builder()
                        .id(UUID.randomUUID().toString())
                        .content("Processed by Kafka Streams: " + inputMessage.getContent())
                        .sender("Kafka Streams Processor")
                        .timestamp(LocalDateTime.now())
                        .type(Message.MessageType.STREAMED)
                        .build();
                
                // Convert back to JSON
                return objectMapper.writeValueAsString(processedMessage);
            } catch (JsonProcessingException e) {
                log.error("Error processing message: {}", e.getMessage(), e);
                return value; // Return original value in case of error
            }
        });

        // Send to output topic
        processedStream.to("stream-output", Produced.with(stringSerde, stringSerde));
        
        return processedStream;
    }
}