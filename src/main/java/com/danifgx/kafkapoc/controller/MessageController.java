package com.danifgx.kafkapoc.controller;

import com.danifgx.kafkapoc.model.Message;
import com.danifgx.kafkapoc.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for sending messages to different messaging systems.
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Send a message using Spring Kafka.
     *
     * @param request The message request containing the content
     * @return The sent message
     */
    @PostMapping("/kafka")
    public ResponseEntity<Message> sendKafkaMessage(@RequestBody MessageRequest request) {
        Message message = messageService.sendKafkaMessage(request.getContent(), "REST API");
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
        Message message = messageService.sendKafkaStreamsMessage(request.getContent(), "REST API");
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
        Message message = messageService.sendCloudStreamKafkaMessage(request.getContent(), "REST API");
        return ResponseEntity.ok(message);
    }

    /**
     * Send a message using Spring Cloud Stream with RabbitMQ.
     *
     * @param request The message request containing the content
     * @return The sent message
     */
    @PostMapping("/cloud-stream-rabbit")
    public ResponseEntity<Message> sendCloudStreamRabbitMessage(@RequestBody MessageRequest request) {
        Message message = messageService.sendCloudStreamRabbitMessage(request.getContent(), "REST API");
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
