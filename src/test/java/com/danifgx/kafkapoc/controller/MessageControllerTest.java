package com.danifgx.kafkapoc.controller;

import com.danifgx.kafkapoc.kafka.KafkaProducerService;
import com.danifgx.kafkapoc.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private StreamBridge streamBridge;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

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

        when(kafkaProducerService.sendMessage(eq(content))).thenReturn(message);

        // When & Then
        mockMvc.perform(post("/api/messages/kafka")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(message.getId()))
                .andExpect(jsonPath("$.content").value(message.getContent()))
                .andExpect(jsonPath("$.sender").value(message.getSender()))
                .andExpect(jsonPath("$.type").value(message.getType().toString()));

        verify(kafkaProducerService).sendMessage(eq(content));
    }

    @Test
    void sendKafkaStreamsMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        // Mock ObjectMapper behavior
        when(objectMapper.writeValueAsString(any(Message.class))).thenReturn("mocked-json");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then
        mockMvc.perform(post("/api/messages/kafka-streams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sender").value("REST API"))
                .andExpect(jsonPath("$.type").value("SIMPLE"));

        verify(objectMapper).writeValueAsString(any(Message.class));
        verify(kafkaTemplate).send(eq("stream-input"), anyString(), eq("mocked-json"));
    }

    @Test
    void sendCloudStreamKafkaMessage_shouldReturnMessage() throws Exception {
        // Given
        String content = "Test message";
        MessageController.MessageRequest request = new MessageController.MessageRequest();
        request.setContent(content);

        when(streamBridge.send(eq("process-in-0"), any(Message.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/messages/cloud-stream-kafka")
                .contentType(MediaType.APPLICATION_JSON)
                .content(realObjectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.sender").value("REST API"))
                .andExpect(jsonPath("$.type").value("SIMPLE"));

        verify(streamBridge).send(eq("process-in-0"), any(Message.class));
    }
}