package com.danifgx.kafkapoc.controller;

import com.danifgx.kafkapoc.kafka.KafkaProducerService;
import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for sending messages to different messaging systems.
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final KafkaProducerService kafkaProducerService;
    private final StreamBridge streamBridge;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Send a message using Spring Kafka.
     *
     * @param request The message request containing the content
     * @return The sent message
     */
    @PostMapping("/kafka")
    public ResponseEntity<Message> sendKafkaMessage(@RequestBody MessageRequest request) {
        Message message = kafkaProducerService.sendMessage(request.getContent());
        return ResponseEntity.ok(message);
    }

    /**
     * Send a message to Kafka Streams.
     *
     * @param request The message request containing the content
     * @return The sent message
     */
    @PostMapping("/kafka-streams")
    public ResponseEntity<Message> sendKafkaStreamsMessage(@RequestBody MessageRequest request) throws JsonProcessingException {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent())
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        String messageJson = objectMapper.writeValueAsString(message);
        kafkaTemplate.send("stream-input", message.getId(), messageJson);
        log.info("Sent message to Kafka Streams: {}", message);

        return ResponseEntity.ok(message);
    }

    /**
     * Send a message using Spring Cloud Stream with Kafka.
     *
     * @param request The message request containing the content
     * @return The sent message
     */
    @PostMapping("/cloud-stream-kafka")
    public ResponseEntity<Message> sendCloudStreamKafkaMessage(@RequestBody MessageRequest request) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent())
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        streamBridge.send("process-in-0", message);
        log.info("Sent message to Spring Cloud Stream (Kafka): {}", message);

        return ResponseEntity.ok(message);
    }

    /**
     * Request object for sending messages.
     */
    public static class MessageRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
