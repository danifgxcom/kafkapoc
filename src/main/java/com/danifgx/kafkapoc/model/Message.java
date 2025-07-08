package com.danifgx.kafkapoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A generic message model that will be used across different messaging implementations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;
    private String content;
    private String sender;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        SIMPLE,
        PROCESSED,
        STREAMED
    }
}