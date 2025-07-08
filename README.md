# Kafka and RabbitMQ Messaging POC

This project is a proof of concept (POC) that demonstrates different messaging strategies using Kafka and RabbitMQ. It showcases three different approaches to working with Kafka:

1. **Kafka Streams** - For stream processing
2. **Spring Cloud Streams** - For a unified programming model across messaging systems
3. **Spring Kafka (Listeners)** - For traditional Kafka messaging

It also demonstrates how Spring Cloud Streams provides a consistent programming model that works with both Kafka and RabbitMQ.

This project uses Kafka with KRaft mode, which is the new consensus mechanism that replaces Zookeeper for managing Kafka clusters. KRaft (Kafka Raft) simplifies the architecture by removing the dependency on Zookeeper, resulting in better performance, scalability, and easier operations.

## Project Structure

- `docker-compose.yml` - Sets up Kafka (with KRaft mode) and RabbitMQ environments
- `src/main/java/com/danifgx/kafkapoc/model/Message.java` - Common message model
- `src/main/java/com/danifgx/kafkapoc/streams/KafkaStreamsProcessor.java` - Kafka Streams implementation
- `src/main/java/com/danifgx/kafkapoc/cloudstream/kafka/KafkaCloudStreamProcessor.java` - Spring Cloud Stream with Kafka
- `src/main/java/com/danifgx/kafkapoc/cloudstream/rabbit/RabbitCloudStreamProcessor.java` - Spring Cloud Stream with RabbitMQ
- `src/main/java/com/danifgx/kafkapoc/kafka/KafkaProducerService.java` - Spring Kafka producer
- `src/main/java/com/danifgx/kafkapoc/kafka/KafkaConsumerService.java` - Spring Kafka consumer
- `src/main/java/com/danifgx/kafkapoc/runner/MessageRunner.java` - Sends test messages on application startup

## Prerequisites

- Java 17
- Docker and Docker Compose
- Maven

## Versions

This project uses the following versions of components:
- Kafka: Latest (with KRaft mode)
- RabbitMQ: 3.12
- Spring Boot: 3.2.12
- Spring Cloud: 2023.0.0

## Running the Application

### 1. Start the Docker Containers

```bash
docker-compose up -d
```

This will start:
- Kafka (ports 9092, 29092) with KRaft mode (no Zookeeper needed)
- Kafka UI (port 8090)
- RabbitMQ (ports 5672, 15672)

### 2. Run the Spring Boot Application

```bash
./mvnw spring-boot:run
```

When the application starts, the `MessageRunner` will automatically send test messages to:
- Kafka using Spring Kafka
- Kafka Streams
- Spring Cloud Stream with Kafka
- Spring Cloud Stream with RabbitMQ

## Monitoring

### Kafka UI

Access the Kafka UI at http://localhost:8090 to monitor Kafka topics, messages, and consumers.

### RabbitMQ Management UI

Access the RabbitMQ Management UI at http://localhost:15672 (username: guest, password: guest) to monitor RabbitMQ queues, exchanges, and messages.

## Implementation Details

### 1. Kafka Streams

The `KafkaStreamsProcessor` class demonstrates how to use the Kafka Streams API to process messages. It:
- Reads messages from the `stream-input` topic
- Processes them (adds a prefix to the content)
- Writes the processed messages to the `stream-output` topic

### 2. Spring Cloud Stream with Kafka

The `KafkaCloudStreamProcessor` class demonstrates how to use Spring Cloud Stream with Kafka. It:
- Defines a function that processes messages
- Automatically binds to the `cloud-stream-topic` and `cloud-stream-topic-processed` topics based on configuration

### 3. Spring Kafka (Listeners)

The `KafkaProducerService` and `KafkaConsumerService` classes demonstrate the traditional way of working with Kafka using Spring Kafka. The producer sends messages to the `simple-messages` topic, and the consumer listens to the same topic using the `@KafkaListener` annotation.

### 4. Spring Cloud Stream with RabbitMQ

The `RabbitCloudStreamProcessor` class demonstrates how Spring Cloud Stream provides a consistent programming model across different messaging systems. The implementation is almost identical to the Kafka version, but it's configured to use RabbitMQ instead.

RabbitMQ integration offers several advantages:
- **Message Durability**: RabbitMQ ensures messages are not lost even if the broker restarts
- **Flexible Routing**: Supports various exchange types (direct, fanout, topic, headers)
- **Dead Letter Queues**: Automatically handles failed messages
- **High Availability**: Supports clustering for fault tolerance
- **Message TTL**: Time-to-live settings for messages
- **Queue Length Limits**: Controls resource usage

The application uses the following RabbitMQ-specific configurations:
- **Exchange Declaration**: Automatically creates exchanges if they don't exist
- **Dead Letter Queue**: Configures DLQ for failed message handling
- **Durability Settings**: Ensures queues and exchanges persist across restarts
- **Consumer Groups**: Enables load balancing across multiple instances

To interact with RabbitMQ messages, you can use the `/api/messages/cloud-stream-rabbit` endpoint.

## Key Takeaways

1. **Kafka Streams** is powerful for stream processing but requires more boilerplate code.
2. **Spring Kafka** provides a simple way to work with Kafka using familiar Spring patterns.
3. **Spring Cloud Stream** provides a unified programming model that works across different messaging systems (Kafka, RabbitMQ, etc.), making it easier to switch between them or use them together.

## Running Tests

The project includes a comprehensive test suite that covers all components:

### Unit Tests

Unit tests verify the behavior of individual components in isolation:

- **Model Tests**: Verify the Message class functionality
- **Service Tests**: Test KafkaProducerService and KafkaConsumerService
- **Processor Tests**: Verify the behavior of all stream processors
- **Controller Tests**: Test the REST endpoints

To run the unit tests:

```bash
./mvnw test
```

### Integration Tests

The project includes a basic integration test that verifies the application context loads correctly:

- **KafkapocIntegrationTest**: A simple test that verifies the application context loads

Note: Running the integration test requires additional configuration and may require actual Kafka and RabbitMQ instances. For this POC, the unit tests provide sufficient coverage of the functionality.

## Stopping the Application

To stop the Docker containers:

```bash
docker-compose down
```
