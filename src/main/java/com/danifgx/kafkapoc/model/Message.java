package com.danifgx.kafkapoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * A generic message model that will be used across different messaging implementations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @NotBlank(message = "Message ID cannot be blank")
    @Size(max = 100, message = "Message ID cannot exceed 100 characters")
    private String id;
    
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;
    
    @NotBlank(message = "Message sender cannot be blank")
    @Size(max = 100, message = "Message sender cannot exceed 100 characters")
    private String sender;
    
    @NotNull(message = "Message timestamp cannot be null")
    private LocalDateTime timestamp;
    
    @NotNull(message = "Message type cannot be null")
    private MessageType type;

    public enum MessageType {
        SIMPLE,
        PROCESSED,
        STREAMED
    }
}