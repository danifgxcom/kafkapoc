package com.danifgx.kafkapoc.controller;

import com.danifgx.kafkapoc.model.Message;
import com.danifgx.kafkapoc.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    private MockMvc mockMvc;
    private ObjectMapper realObjectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
        realObjectMapper = new ObjectMapper();
        realObjectMapper.findAndRegisterModules(); // To handle LocalDateTime
    }

    @Test
    void sendKafkaMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("Spring Kafka Producer")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        when(messageService.sendKafkaMessage(content, "REST API")).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/messages/kafka")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.getId()))
                .andExpect(jsonPath("$.content").value(message.getContent()))
                .andExpect(jsonPath("$.sender").value(message.getSender()))
                .andExpect(jsonPath("$.type").value(message.getType().toString()));

        verify(messageService).sendKafkaMessage(content, "REST API");
    }

    @Test
    void sendKafkaStreamsMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        when(messageService.sendKafkaStreamsMessage(content, "REST API")).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/messages/kafka-streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sender").value("REST API"))
                .andExpect(jsonPath("$.type").value("SIMPLE"));

        verify(messageService).sendKafkaStreamsMessage(content, "REST API");
    }

    @Test
    void sendCloudStreamKafkaMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        when(messageService.sendCloudStreamKafkaMessage(content, "REST API")).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/messages/cloud-stream-kafka")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sender").value("REST API"))
                .andExpect(jsonPath("$.type").value("SIMPLE"));

        verify(messageService).sendCloudStreamKafkaMessage(content, "REST API");
    }

    @Test
    void sendCloudStreamRabbitMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        when(messageService.sendCloudStreamRabbitMessage(content, "REST API")).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/messages/cloud-stream-rabbit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sender").value("REST API"))
                .andExpect(jsonPath("$.type").value("SIMPLE"));

        verify(messageService).sendCloudStreamRabbitMessage(content, "REST API");
    }
}
