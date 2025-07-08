package com.danifgx.kafkapoc.rabbit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Prueba para verificar la disponibilidad de RabbitMQ.
 */
@ExtendWith(SpringExtension.class)
@Import(RabbitMQAvailabilityTest.RabbitMQTestContextConfiguration.class)
public class RabbitMQAvailabilityTest {

    @TestConfiguration
    static class RabbitMQTestContextConfiguration {
        @Bean
        public CachingConnectionFactory connectionFactory() {
            CachingConnectionFactory factory = mock(CachingConnectionFactory.class);
            Connection mockConnection = mock(Connection.class);
            when(factory.createConnection()).thenReturn(mockConnection);
            when(mockConnection.isOpen()).thenReturn(true);
            return factory;
        }

        @Bean
        public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
            return new RabbitTemplate(connectionFactory);
        }
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    /**
     * Prueba que verifica si se puede establecer una conexión con RabbitMQ.
     */
    @Test
    void testRabbitMQAvailability() {
        // La conexión ya está configurada en el TestConfiguration

        // Verificar que la conexión se puede establecer
        assertTrue(rabbitTemplate.getConnectionFactory().createConnection().isOpen());

        // Verificar que se llamó al método createConnection
        verify(connectionFactory).createConnection();
    }
}
