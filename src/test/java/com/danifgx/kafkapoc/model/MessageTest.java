package com.danifgx.kafkapoc.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testMessageBuilder() {
        // Given
        String id = "test-id";
        String content = "test-content";
        String sender = "test-sender";
        LocalDateTime timestamp = LocalDateTime.now();
        Message.MessageType type = Message.MessageType.SIMPLE;

        // When
        Message message = Message.builder()
                .id(id)
                .content(content)
                .sender(sender)
                .timestamp(timestamp)
                .type(type)
                .build();

        // Then
        assertEquals(id, message.getId());
        assertEquals(content, message.getContent());
        assertEquals(sender, message.getSender());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(type, message.getType());
    }

    @Test
    void testMessageGettersAndSetters() {
        // Given
        Message message = new Message();
        String id = "test-id";
        String content = "test-content";
        String sender = "test-sender";
        LocalDateTime timestamp = LocalDateTime.now();
        Message.MessageType type = Message.MessageType.PROCESSED;

        // When
        message.setId(id);
        message.setContent(content);
        message.setSender(sender);
        message.setTimestamp(timestamp);
        message.setType(type);

        // Then
        assertEquals(id, message.getId());
        assertEquals(content, message.getContent());
        assertEquals(sender, message.getSender());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(type, message.getType());
    }

    @Test
    void testMessageEqualsAndHashCode() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Message message1 = Message.builder()
                .id("1")
                .content("content")
                .sender("sender")
                .timestamp(now)
                .type(Message.MessageType.SIMPLE)
                .build();

        Message message2 = Message.builder()
                .id("1")
                .content("content")
                .sender("sender")
                .timestamp(now)
                .type(Message.MessageType.SIMPLE)
                .build();

        Message message3 = Message.builder()
                .id("2")
                .content("different")
                .sender("different")
                .timestamp(now)
                .type(Message.MessageType.PROCESSED)
                .build();

        // Then
        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
        assertNotEquals(message1, message3);
        assertNotEquals(message1.hashCode(), message3.hashCode());
    }

    @Test
    void testMessageToString() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .id("test-id")
                .content("test-content")
                .sender("test-sender")
                .timestamp(now)
                .type(Message.MessageType.STREAMED)
                .build();

        // When
        String toString = message.toString();

        // Then
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("test-content"));
        assertTrue(toString.contains("test-sender"));
        assertTrue(toString.contains(now.toString()));
        assertTrue(toString.contains("STREAMED"));
    }
}