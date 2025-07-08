package com.danifgx.kafkapoc.cloudstream.rabbit;

import com.danifgx.kafkapoc.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * Prueba de integración para RabbitMQ que verifica el correcto funcionamiento del procesador.
 */
@ExtendWith(SpringExtension.class)
public class RabbitMQIntegrationTest {

    private RabbitCloudStreamProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RabbitCloudStreamProcessor();
    }

    /**
     * Prueba que el procesador RabbitMQ transforma correctamente un mensaje.
     */
    @Test
    void testRabbitMessageProcessing() {
        // Crear una función a partir del bean del procesador
        Function<Message, Message> processFunction = processor.processRabbit();

        // Crear un mensaje de prueba
        Message testMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Mensaje de prueba para RabbitMQ")
                .sender("Test")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // Procesar el mensaje
        Message result = processFunction.apply(testMessage);

        // Verificar el resultado (el contenido debe haber sido modificado por el procesador)
        assert result.getContent().contains("Processed by Spring Cloud Stream (RabbitMQ)");
        assert result.getSender().equals("Spring Cloud Stream RabbitMQ Processor");
        assert result.getType() == Message.MessageType.PROCESSED;
    }
}
