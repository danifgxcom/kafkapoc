package com.danifgx.kafkapoc.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Configuración de prueba para RabbitMQ.
 * Proporciona mocks para los componentes de RabbitMQ en pruebas.
 */
@TestConfiguration
@Profile("test")
public class RabbitMQTestConfig {

    /**
     * Crea un mock de ConnectionFactory para RabbitMQ en las pruebas.
     *
     * @return Un mock de ConnectionFactory
     */
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory mockConnectionFactory = mock(CachingConnectionFactory.class);
        Connection mockConnection = mock(Connection.class);

        when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);
        when(mockConnection.isOpen()).thenReturn(true);

        return mockConnectionFactory;
    }

    /**
     * Crea un RabbitTemplate que utiliza el ConnectionFactory mockeado.
     *
     * @param connectionFactory El ConnectionFactory mockeado
     * @return Un RabbitTemplate configurado para pruebas
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}
