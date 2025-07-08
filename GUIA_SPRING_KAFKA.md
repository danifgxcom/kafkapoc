# Guía Completa: Spring Boot con Kafka, Cloud Stream y RabbitMQ

## Índice
1. [Introducción](#introducción)
2. [Configuración del Proyecto](#configuración-del-proyecto)
3. [Implementación con Spring Kafka](#implementación-con-spring-kafka)
4. [Implementación con Kafka Streams](#implementación-con-kafka-streams)
5. [Implementación con Spring Cloud Stream](#implementación-con-spring-cloud-stream)
6. [Integración con RabbitMQ](#integración-con-rabbitmq)
7. [Configuración y Propiedades](#configuración-y-propiedades)
8. [Testing](#testing)
9. [Mejores Prácticas](#mejores-prácticas)
10. [Comparación de Enfoques](#comparación-de-enfoques)
11. [Ejemplos de Curl para Probar los Endpoints](#ejemplos-de-curl-para-probar-los-endpoints)
12. [Documentación OpenAPI](#documentación-openapi)

---

## Introducción

Este proyecto demuestra **cuatro enfoques diferentes** para implementar mensajería en Spring Boot:

1. **Spring Kafka**: Integración directa con Apache Kafka
2. **Kafka Streams**: Procesamiento de flujos de datos en tiempo real
3. **Spring Cloud Stream con Kafka**: Abstracción de alto nivel sobre Kafka
4. **Spring Cloud Stream con RabbitMQ**: Abstracción sobre RabbitMQ

### ¿Por qué diferentes enfoques?

- **Spring Kafka**: Control granular, máximo rendimiento
- **Kafka Streams**: Procesamiento de streams complejos
- **Spring Cloud Stream**: Abstracción, facilidad de cambio de broker
- **RabbitMQ**: Diferentes patrones de mensajería, transacciones ACID

---

## Configuración del Proyecto

### Dependencias Maven (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Kafka Streams -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-streams</artifactId>
    </dependency>

    <!-- Spring Cloud Stream -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream</artifactId>
    </dependency>

    <!-- Kafka Binder para Cloud Stream -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-kafka</artifactId>
    </dependency>

    <!-- Kafka Streams Binder -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-kafka-streams</artifactId>
    </dependency>

    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### Modelo de Datos

```java
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
```

**Características del modelo:**
- **Lombok**: Reduce boilerplate con `@Data`, `@Builder`
- **Jackson**: Serialización/deserialización JSON automática
- **Inmutable**: Uso de `@Builder` para crear instancias
- **Tipo de mensaje**: Enum para identificar el origen

---

## Implementación con Spring Kafka

La implementación con Spring Kafka proporciona una integración directa con Apache Kafka, ofreciendo control granular sobre la configuración y el comportamiento de los productores y consumidores.

### 1. Configuración de Jackson

La configuración de Jackson es esencial para manejar correctamente la serialización y deserialización de objetos Java a JSON y viceversa, especialmente cuando se trabaja con tipos de datos como `LocalDateTime`.

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RecordMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        // Use StringJsonMessageConverter instead of JsonMessageConverter
        // This will handle both JSON objects and strings
        return new StringJsonMessageConverter(objectMapper);
    }
}
```

**Análisis detallado del código:**
- **[@Configuration](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/config/JacksonConfig.java#L14)**: Esta anotación indica a Spring que esta clase contiene definiciones de beans que deben ser procesadas por el contenedor de Spring.
- **[objectMapper()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/config/JacksonConfig.java#L17-L22)**: Este método crea y configura un bean `ObjectMapper` de Jackson:
  1. Crea una nueva instancia de `ObjectMapper`
  2. Registra el módulo `JavaTimeModule`, que proporciona soporte para las clases de fecha y hora de Java 8 (como `LocalDateTime`)
  3. Devuelve el mapper configurado
- **[jsonMessageConverter()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/config/JacksonConfig.java#L24-L29)**: Este método crea un convertidor de mensajes para Kafka:
  1. Crea una instancia de `StringJsonMessageConverter` que utiliza el `ObjectMapper` configurado
  2. Este convertidor maneja tanto objetos JSON como cadenas de texto simples
  3. Permite la conversión automática entre mensajes Kafka y objetos Java

**¿Por qué esta configuración?**
- **JavaTimeModule**: Necesario para serializar/deserializar correctamente los tipos de fecha y hora de Java 8 como `LocalDateTime`. Sin este módulo, Jackson no sabría cómo manejar estos tipos.
- **StringJsonMessageConverter**: A diferencia de `JsonMessageConverter`, puede manejar tanto objetos JSON como cadenas de texto simples, lo que proporciona mayor flexibilidad.

### 2. Productor de Mensajes

El productor de mensajes es responsable de crear y enviar mensajes a un topic de Kafka. Spring Kafka proporciona `KafkaTemplate` como abstracción principal para enviar mensajes.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Sends a message to the simple-messages topic.
     *
     * @param content The content of the message
     * @return The sent message
     */
    public Message sendMessage(String content) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender("Spring Kafka Producer")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("simple-messages", message.getId(), messageJson);
            log.info("Sent message to Kafka: {}", message);
            return message;
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending message to Kafka", e);
        }
    }
}
```

**Análisis paso a paso del método [sendMessage()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/kafka/KafkaProducerService.java#L31-L49):**

1. **Creación del mensaje**:
   - Utiliza el patrón Builder de Lombok para crear una instancia de `Message`
   - Genera un ID único con `UUID.randomUUID()`
   - Establece el contenido proporcionado por el usuario
   - Define el remitente como "Spring Kafka Producer"
   - Establece la marca de tiempo actual
   - Define el tipo de mensaje como `SIMPLE`

2. **Serialización y envío**:
   - Convierte el objeto `Message` a una cadena JSON utilizando `objectMapper.writeValueAsString()`
   - Envía el mensaje al topic "simple-messages" utilizando `kafkaTemplate.send()`
   - Utiliza el ID del mensaje como clave de partición
   - Registra el envío exitoso en los logs
   - Devuelve el mensaje enviado

3. **Manejo de errores**:
   - Captura excepciones de tipo `JsonProcessingException` que pueden ocurrir durante la serialización
   - Registra el error en los logs
   - Convierte la excepción checked en una excepción runtime para simplificar el manejo de errores

**Puntos clave:**
- **KafkaTemplate**: Es la abstracción principal de Spring para enviar mensajes a Kafka. Maneja internamente la creación de productores, la serialización y el envío de mensajes.
- **Serialización manual**: Al serializar manualmente el objeto a JSON, tenemos control completo sobre el formato y podemos manejar errores de serialización.
- **Clave del mensaje**: Usar el UUID como clave de partición garantiza una distribución uniforme de los mensajes entre las particiones del topic.
- **Manejo de errores**: La conversión de excepciones checked a runtime simplifica el código cliente y sigue las mejores prácticas de Spring.

### 3. Consumidor de Mensajes

El consumidor de mensajes es responsable de recibir y procesar mensajes de un topic de Kafka. Spring Kafka proporciona la anotación `@KafkaListener` para simplificar la creación de consumidores.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;

    /**
     * Listens for messages on the simple-messages topic.
     *
     * @param messageJson The JSON message from Kafka
     */
    @KafkaListener(topics = "simple-messages", groupId = "simple-consumer-group")
    public void consumeMessage(String messageJson) {
        try {
            // Check if the message is a JSON object or a string
            Message message;
            try {
                // First try to parse as a JSON object
                JsonNode jsonNode = objectMapper.readTree(messageJson);
                if (jsonNode.isObject()) {
                    message = objectMapper.readValue(messageJson, Message.class);
                } else {
                    // If it's not an object, treat it as a string
                    message = Message.builder()
                            .content(messageJson)
                            .build();
                }
            } catch (JsonProcessingException e) {
                // If parsing fails, treat it as a string
                message = Message.builder()
                        .content(messageJson)
                        .build();
            }

            log.info("Received message from Kafka: {}", message);

            // In a real application, you would process the message here
            processMessage(message);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
        }
    }

    /**
     * Process the received message.
     * This is a placeholder for actual message processing logic.
     *
     * @param message The message to process
     */
    private void processMessage(Message message) {
        log.info("Processing message with Spring Kafka Listener: {}", message.getContent());
        // Add your business logic here
    }
}
```

**Análisis paso a paso del método [consumeMessage()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/kafka/KafkaConsumerService.java#L27-L57):**

1. **Recepción del mensaje**:
   - La anotación `@KafkaListener` configura el método para escuchar mensajes del topic "simple-messages"
   - El parámetro `groupId` define el grupo de consumidores al que pertenece este consumidor
   - El método recibe el mensaje como una cadena JSON

2. **Deserialización robusta**:
   - Intenta analizar el mensaje como un objeto JSON utilizando `objectMapper.readTree()`
   - Si es un objeto JSON válido, lo deserializa a un objeto `Message`
   - Si no es un objeto JSON o si ocurre un error de análisis, crea un objeto `Message` con el contenido original

3. **Procesamiento del mensaje**:
   - Registra la recepción del mensaje en los logs
   - Llama al método `processMessage()` para procesar el mensaje
   - En una aplicación real, aquí se implementaría la lógica de negocio

4. **Manejo de errores**:
   - Captura cualquier excepción que pueda ocurrir durante el procesamiento
   - Registra el error en los logs para facilitar la depuración

**Análisis del método [processMessage()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/kafka/KafkaConsumerService.java#L65-L68):**
- Este método es un placeholder para la lógica de procesamiento real
- Registra el contenido del mensaje en los logs
- En una aplicación real, aquí se implementaría la lógica de negocio específica

**Características del consumidor:**
- **@KafkaListener**: Esta anotación simplifica enormemente la creación de consumidores Kafka, manejando automáticamente la suscripción al topic, la deserialización y la entrega de mensajes.
- **Consumer Group**: El uso de grupos de consumidores permite el balanceo de carga automático entre múltiples instancias del mismo consumidor, aumentando la escalabilidad.
- **Deserialización robusta**: El código maneja diferentes formatos de mensaje y errores de deserialización, lo que aumenta la resiliencia del consumidor.
- **Manejo de errores**: El manejo adecuado de excepciones evita que un mensaje mal formado detenga el procesamiento de mensajes subsiguientes.

### 4. Configuración de Kafka en application.properties

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

**Explicación de propiedades:**
- **bootstrap-servers**: Dirección del cluster Kafka
- **auto-offset-reset**: Comportamiento al no encontrar offset (`earliest`, `latest`)
- **serializers/deserializers**: Conversión de claves y valores

---

## Implementación con Kafka Streams

Kafka Streams es una biblioteca de procesamiento de flujos de datos en tiempo real que forma parte del ecosistema Apache Kafka. Permite construir aplicaciones y microservicios que procesan y transforman datos almacenados en Kafka.

### 1. Procesador de Streams

El componente principal de nuestra implementación con Kafka Streams es la clase [KafkaStreamsProcessor](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/streams/KafkaStreamsProcessor.java), que define la topología de procesamiento:

```java
@Slf4j
@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
public class KafkaStreamsProcessor {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder) {
        // Create JSON Serde for Message class
        final Serde<String> stringSerde = Serdes.String();
        final JsonSerde<Message> messageSerde = new JsonSerde<>(Message.class, objectMapper);

        // Create a stream from the input topic
        KStream<String, String> inputStream = streamsBuilder.stream(
                "stream-input", 
                Consumed.with(stringSerde, stringSerde)
        );

        // Process the stream
        KStream<String, String> processedStream = inputStream.mapValues(value -> {
            try {
                // Parse the input message
                Message inputMessage = objectMapper.readValue(value, Message.class);
                log.info("Processing message with Kafka Streams: {}", inputMessage);

                // Create a processed message
                Message processedMessage = Message.builder()
                        .id(UUID.randomUUID().toString())
                        .content("Processed by Kafka Streams: " + inputMessage.getContent())
                        .sender("Kafka Streams Processor")
                        .timestamp(LocalDateTime.now())
                        .type(Message.MessageType.STREAMED)
                        .build();

                // Convert back to JSON
                return objectMapper.writeValueAsString(processedMessage);
            } catch (JsonProcessingException e) {
                log.error("Error processing message: {}", e.getMessage(), e);
                return value; // Return original value in case of error
            }
        });

        // Send to output topic
        processedStream.to("stream-output", Produced.with(stringSerde, stringSerde));

        return processedStream;
    }
}
```

### 2. Análisis detallado del procesador de streams

#### Anotaciones y configuración

- **@Configuration**: Indica a Spring que esta clase contiene definiciones de beans.
- **@EnableKafkaStreams**: Habilita el soporte de Kafka Streams en la aplicación Spring Boot.
- **@RequiredArgsConstructor**: Genera un constructor con los campos finales como parámetros.
- **@Slf4j**: Proporciona un logger para la clase.

#### Método kStream

El método [kStream()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/streams/KafkaStreamsProcessor.java#L35-L75) define la topología de procesamiento de streams y se puede dividir en varias partes:

1. **Configuración de serializadores/deserializadores (Serdes)**:

   Los `Serde` (Serializer/Deserializer) son componentes que convierten entre los objetos Java y los bytes que Kafka almacena:
   - `stringSerde` maneja la serialización/deserialización de cadenas de texto
   - `messageSerde` maneja la serialización/deserialización de objetos `Message` a/desde JSON

2. **Creación del stream de entrada**:

   - `streamsBuilder.stream()` crea un `KStream` que consume mensajes del topic "stream-input"
   - `Consumed.with()` especifica los serdes a utilizar para la clave y el valor
   - El resultado es un flujo continuo de mensajes que podemos procesar

3. **Procesamiento del stream**:

   - `mapValues()` transforma solo los valores del stream, manteniendo las claves intactas
   - Para cada mensaje:
     1. Deserializa el JSON a un objeto `Message`
     2. Registra el mensaje en los logs
     3. Crea un nuevo mensaje procesado con contenido modificado
     4. Serializa el mensaje procesado de vuelta a JSON
     5. En caso de error, devuelve el mensaje original sin procesar

4. **Envío al topic de salida**:

   - `to()` envía los mensajes procesados al topic "stream-output"
   - `Produced.with()` especifica los serdes a utilizar para la serialización

5. **Retorno del stream procesado**:

   - Devuelve el stream procesado como un bean de Spring, lo que permite que otros componentes lo utilicen o que se puedan aplicar más transformaciones

### 3. Conceptos clave de Kafka Streams

- **KStream**: Representa un flujo infinito de registros clave-valor. Es la abstracción principal para trabajar con datos en Kafka Streams.

- **Topology**: Es el grafo de procesamiento que define cómo se transforman los datos. Consiste en nodos (procesadores) conectados por aristas (streams).

- **Operaciones sin estado (Stateless)**: 
  - `map`/`mapValues`: Transforma cada registro individualmente
  - `filter`: Filtra registros según una condición
  - `peek`: Realiza una acción para cada registro sin modificarlo (útil para logging)
  - `flatMap`/`flatMapValues`: Transforma cada registro en 0, 1 o más registros

- **Operaciones con estado (Stateful)**:
  - `aggregate`: Acumula valores en un resultado
  - `count`: Cuenta ocurrencias
  - `join`: Combina streams basándose en claves comunes
  - `reduce`: Combina valores con la misma clave
  - `windowedBy`: Agrupa registros en ventanas de tiempo

### 4. Ventajas de Kafka Streams

- **Procesamiento en tiempo real**: Procesa los datos a medida que llegan, sin necesidad de almacenamiento intermedio.
- **Escalabilidad**: Se escala horizontalmente añadiendo más instancias de la aplicación.
- **Tolerancia a fallos**: Mantiene el estado en topics de Kafka, lo que permite recuperarse de fallos.
- **Exactly-once semantics**: Garantiza que cada registro se procese exactamente una vez.
- **Sin dependencias externas**: No requiere un cluster separado como Spark o Flink.
- **Integración con Spring Boot**: Fácil de configurar y usar en aplicaciones Spring Boot.

### 5. Configuración en application.properties

```properties
# Kafka Streams Configuration
spring.cloud.stream.kafka.streams.binder.configuration.default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
spring.cloud.stream.kafka.streams.binder.configuration.default.value.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
spring.cloud.stream.kafka.streams.binder.configuration.commit.interval.ms=1000

# Application ID (importante para el escalado y la recuperación de estado)
spring.cloud.stream.kafka.streams.binder.configuration.application.id=kafka-streams-demo

# Configuración de estado (para operaciones stateful)
spring.cloud.stream.kafka.streams.binder.configuration.state.dir=/tmp/kafka-streams
```

**Explicación de propiedades importantes**:

- **default.key.serde/default.value.serde**: Define los serializadores/deserializadores por defecto.
- **commit.interval.ms**: Frecuencia con la que se confirman los offsets procesados.
- **application.id**: Identificador único de la aplicación Kafka Streams (crucial para el escalado).
- **state.dir**: Directorio donde se almacena el estado local (para operaciones stateful).

### 6. Casos de uso comunes

- **Enriquecimiento de datos**: Añadir información adicional a los mensajes.
- **Filtrado**: Eliminar mensajes que no cumplen ciertos criterios.
- **Agregación**: Calcular estadísticas como sumas, promedios, etc.
- **Detección de anomalías**: Identificar patrones inusuales en tiempo real.
- **Joins**: Combinar datos de diferentes topics.
- **Ventanas temporales**: Analizar datos en intervalos de tiempo específicos.

---

## Implementación con Spring Cloud Stream

Spring Cloud Stream es un framework que proporciona una capa de abstracción sobre los sistemas de mensajería como Kafka y RabbitMQ. Permite desarrollar aplicaciones de procesamiento de streams sin depender de la implementación específica del broker de mensajes.

### 1. Modelo de programación funcional

Spring Cloud Stream 3.0+ utiliza un modelo de programación funcional basado en las interfaces funcionales de Java 8:

- **Supplier<T>**: Produce mensajes (source)
- **Consumer<T>**: Consume mensajes (sink)
- **Function<T, R>**: Procesa mensajes, transformando entrada en salida (processor)

Este modelo permite una gran flexibilidad y composición de funciones, facilitando la creación de pipelines de procesamiento complejos.

### 2. Procesador con Cloud Stream (Kafka)

El componente principal de nuestra implementación con Spring Cloud Stream es la clase [KafkaCloudStreamProcessor](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/kafka/KafkaCloudStreamProcessor.java), que define una función de procesamiento:

```java
/**
 * Spring Cloud Stream processor for Kafka.
 * This demonstrates how to use Spring Cloud Stream with Kafka.
 * It processes messages from cloud-stream-topic and sends them to cloud-stream-topic-processed.
 */
@Slf4j
@Configuration
public class KafkaCloudStreamProcessor {

    /**
     * Defines a processor function that takes a Message as input and returns a processed Message.
     * The binding is configured in application.properties:
     * - Input: process-in-0 -> cloud-stream-topic
     * - Output: process-out-0 -> cloud-stream-topic-processed
     */
    @Bean
    public Function<Message, Message> process() {
        return message -> {
            log.info("Processing message with Spring Cloud Stream (Kafka): {}", message);

            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content("Processed by Spring Cloud Stream (Kafka): " + message.getContent())
                    .sender("Spring Cloud Stream Processor")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.PROCESSED)
                    .build();
        };
    }
}
```

### 3. Análisis detallado del procesador

#### Anotaciones y estructura

- **[@Configuration](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/kafka/KafkaCloudStreamProcessor.java#L18)**: Indica a Spring que esta clase contiene definiciones de beans.
- **[@Slf4j](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/kafka/KafkaCloudStreamProcessor.java#L17)**: Proporciona un logger para la clase.

#### Método process()

El método [process()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/kafka/KafkaCloudStreamProcessor.java#L27-L40) define una función que procesa mensajes:

1. **Definición de la función**:
   - Retorna una implementación de `Function<Message, Message>`, que toma un mensaje como entrada y produce un mensaje como salida.
   - La función se registra como un bean de Spring, lo que permite que Spring Cloud Stream la conecte automáticamente a los canales de entrada y salida.

2. **Procesamiento del mensaje**:
   - Registra el mensaje recibido en los logs.
   - Crea un nuevo mensaje con:
     - Un nuevo ID generado con UUID.
     - El contenido original modificado.
     - Un remitente específico.
     - Una marca de tiempo actual.
     - Un tipo de mensaje `PROCESSED`.
   - Retorna el mensaje procesado.

3. **Binding automático**:
   - Spring Cloud Stream conecta automáticamente esta función a los canales definidos en la configuración.
   - El nombre del bean (`process`) determina los nombres de los canales: `process-in-0` para la entrada y `process-out-0` para la salida.

### 4. Configuración de bindings

La configuración de los bindings se realiza en el archivo `application.properties`:

```properties
# Spring Cloud Stream Binder Configuration
spring.cloud.stream.default-binder=kafka
spring.cloud.stream.binders.kafka.type=kafka
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.brokers=localhost:29092
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.auto-create-topics=true

# Spring Cloud Stream Kafka Bindings
spring.cloud.stream.bindings.process-in-0.destination=cloud-stream-topic
spring.cloud.stream.bindings.process-out-0.destination=cloud-stream-topic-processed
spring.cloud.stream.bindings.process-in-0.binder=kafka
spring.cloud.stream.bindings.process-out-0.binder=kafka
```

**Explicación de la configuración**:

- **default-binder**: Define el binder predeterminado (Kafka en este caso).
- **binders.kafka.type**: Especifica el tipo de binder.
- **binders.kafka.environment**: Configura el entorno del binder, como los brokers de Kafka.
- **bindings.process-in-0.destination**: Mapea el canal de entrada a un topic de Kafka.
- **bindings.process-out-0.destination**: Mapea el canal de salida a un topic de Kafka.
- **bindings.process-in-0.binder**: Especifica qué binder usar para el canal de entrada.
- **bindings.process-out-0.binder**: Especifica qué binder usar para el canal de salida.

### 5. Envío de mensajes con StreamBridge

Para enviar mensajes a un procesador de Spring Cloud Stream desde código imperativo (como un controlador REST), se utiliza la clase `StreamBridge`:

```java
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final StreamBridge streamBridge;

    @PostMapping("/cloud-stream-kafka")
    public ResponseEntity<Message> sendCloudStreamKafkaMessage(@RequestBody MessageRequest request) {
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(request.getContent())
                .sender("REST API")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        streamBridge.send("process-in-0", message);
        log.info("Sent message to Spring Cloud Stream (Kafka): {}", message);

        return ResponseEntity.ok(message);
    }
}
```

**Análisis del método [sendCloudStreamKafkaMessage()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/controller/MessageController.java#L75-L89)**:

1. **Creación del mensaje**:
   - Crea un objeto `Message` con los datos de la solicitud.
   - Genera un ID único, establece el remitente, la marca de tiempo y el tipo.

2. **Envío del mensaje**:
   - Utiliza `streamBridge.send()` para enviar el mensaje al canal `process-in-0`.
   - El primer parámetro es el nombre del canal de destino.
   - El segundo parámetro es el mensaje a enviar.

3. **Respuesta**:
   - Devuelve el mensaje enviado como respuesta HTTP.

### 6. Ventajas de Spring Cloud Stream

- **Abstracción del broker**: El código no depende de la implementación específica del broker (Kafka, RabbitMQ, etc.).
- **Modelo declarativo**: La configuración se realiza principalmente a través de propiedades, no de código.
- **Programación funcional**: Utiliza el modelo de programación funcional de Java 8, lo que facilita la composición de funciones.
- **Binding automático**: Conecta automáticamente las funciones a los canales de entrada y salida.
- **Cambio de broker sin cambios de código**: Se puede cambiar el broker subyacente simplemente modificando la configuración.
- **Serialización/deserialización automática**: Convierte automáticamente entre objetos Java y mensajes del broker.

---

## Integración con RabbitMQ

Spring Cloud Stream permite cambiar fácilmente entre diferentes brokers de mensajería, como Kafka y RabbitMQ, sin cambiar el código de la aplicación. En esta sección, veremos cómo integrar RabbitMQ como broker de mensajería utilizando Spring Cloud Stream.

### 1. Procesador con RabbitMQ

El componente principal de nuestra implementación con RabbitMQ es la clase [RabbitCloudStreamProcessor](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/rabbit/RabbitCloudStreamProcessor.java), que define una función de procesamiento muy similar a la que usamos con Kafka:

```java
/**
 * Spring Cloud Stream processor for RabbitMQ.
 * This demonstrates how to use Spring Cloud Stream with RabbitMQ.
 * It processes messages from rabbit-input-queue and sends them to rabbit-output-queue.
 * The implementation is almost identical to the Kafka version, showing the power of Spring Cloud Stream's abstraction.
 */
@Slf4j
@Configuration
public class RabbitCloudStreamProcessor {

    /**
     * Defines a processor function that takes a Message as input and returns a processed Message.
     * The binding is configured in application.properties:
     * - Input: processRabbit-in-0 -> rabbit-input-queue
     * - Output: processRabbit-out-0 -> rabbit-output-queue
     */
    @Bean
    public Function<Message, Message> processRabbit() {
        return message -> {
            log.info("Processing message with Spring Cloud Stream (RabbitMQ): {}", message);

            return Message.builder()
                    .id(UUID.randomUUID().toString())
                    .content("Processed by Spring Cloud Stream (RabbitMQ): " + message.getContent())
                    .sender("Spring Cloud Stream RabbitMQ Processor")
                    .timestamp(LocalDateTime.now())
                    .type(Message.MessageType.PROCESSED)
                    .build();
        };
    }
}
```

### 2. Análisis detallado del procesador con RabbitMQ

La implementación del procesador con RabbitMQ es casi idéntica a la versión de Kafka, lo que demuestra el poder de la abstracción proporcionada por Spring Cloud Stream.

#### Método processRabbit()

El método [processRabbit()](https://github.com/danifgx/projects/blob/main/kafkapoc/src/main/java/com/danifgx/kafkapoc/cloudstream/rabbit/RabbitCloudStreamProcessor.java#L29-L41) define una función que procesa mensajes:

1. **Definición de la función**:
   - Retorna una implementación de `Function<Message, Message>`, igual que en la versión de Kafka.
   - La función se registra como un bean de Spring con el nombre `processRabbit`.

2. **Procesamiento del mensaje**:
   - Registra el mensaje recibido en los logs.
   - Crea un nuevo mensaje con:
     - Un nuevo ID generado con UUID.
     - El contenido original modificado, indicando que fue procesado por RabbitMQ.
     - Un remitente específico para RabbitMQ.
     - Una marca de tiempo actual.
     - Un tipo de mensaje `PROCESSED`.
   - Retorna el mensaje procesado.

3. **Binding automático**:
   - Spring Cloud Stream conecta automáticamente esta función a los canales definidos en la configuración.
   - El nombre del bean (`processRabbit`) determina los nombres de los canales: `processRabbit-in-0` para la entrada y `processRabbit-out-0` para la salida.

### 3. Configuración de RabbitMQ

La configuración de RabbitMQ se realiza en el archivo `application.properties`:

```properties
# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# RabbitMQ Binder
spring.cloud.stream.binders.rabbit.type=rabbit
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.host=localhost
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.port=5672
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.username=guest
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.password=guest

# Spring Cloud Stream RabbitMQ Bindings
spring.cloud.stream.bindings.processRabbit-in-0.destination=rabbit-input-queue
spring.cloud.stream.bindings.processRabbit-out-0.destination=rabbit-output-queue
spring.cloud.stream.bindings.processRabbit-in-0.binder=rabbit
spring.cloud.stream.bindings.processRabbit-out-0.binder=rabbit
```

**Explicación de la configuración**:

- **spring.rabbitmq**: Configura la conexión básica a RabbitMQ (host, puerto, credenciales).
- **spring.cloud.stream.binders.rabbit.type**: Especifica el tipo de binder como `rabbit`.
- **spring.cloud.stream.binders.rabbit.environment**: Configura el entorno del binder RabbitMQ.
- **spring.cloud.stream.bindings.processRabbit-in-0.destination**: Mapea el canal de entrada a una cola de RabbitMQ.
- **spring.cloud.stream.bindings.processRabbit-out-0.destination**: Mapea el canal de salida a una cola de RabbitMQ.
- **spring.cloud.stream.bindings.processRabbit-in-0.binder**: Especifica que se debe usar el binder `rabbit` para el canal de entrada.
- **spring.cloud.stream.bindings.processRabbit-out-0.binder**: Especifica que se debe usar el binder `rabbit` para el canal de salida.

### 4. Diferencias entre Kafka y RabbitMQ

| Característica | Kafka | RabbitMQ |
|---------------|--------|----------|
| **Modelo** | Pub/Sub + Log | AMQP |
| **Persistencia** | Disco (log) | Memoria + disco |
| **Throughput** | Muy alto | Alto |
| **Latencia** | Baja | Muy baja |
| **Durabilidad** | Excelente | Buena |
| **Casos de uso** | Streaming, Event sourcing | Microservicios, RPC |
| **Ordenamiento** | Por partición | Por cola |
| **Redelivery** | No automático | Automático |
| **Patrones de routing** | Limitados | Muy flexibles |
| **Transacciones** | Limitadas | ACID completas |

### 5. Ventajas de usar RabbitMQ con Spring Cloud Stream

- **Patrones de routing avanzados**: RabbitMQ ofrece exchanges de tipo direct, fanout, topic y headers.
- **Confirmaciones de mensajes**: Acknowledgments automáticos o manuales.
- **Transacciones ACID**: Soporte para transacciones completas.
- **Baja latencia**: Ideal para comunicaciones que requieren respuesta rápida.
- **Colas con prioridad**: Permite procesar mensajes más importantes primero.
- **Dead Letter Queues**: Manejo automático de mensajes fallidos.
- **TTL (Time-To-Live)**: Expiración automática de mensajes.
- **Plugins extensibles**: Gran ecosistema de plugins para extender funcionalidad.

### 6. Casos de uso ideales para RabbitMQ

- **Microservicios síncronos**: Cuando se requiere respuesta inmediata.
- **Operaciones transaccionales**: Cuando se necesita garantía ACID.
- **Routing complejo**: Cuando los mensajes deben enrutarse según múltiples criterios.
- **Priorización de mensajes**: Cuando algunos mensajes son más importantes que otros.
- **Aplicaciones de baja latencia**: Cuando el tiempo de respuesta es crítico.
- **Sistemas de notificación**: Para enviar alertas y notificaciones en tiempo real.

---

## Configuración y Propiedades

### Archivo application.properties completo

A continuación se muestra el archivo de configuración completo que incluye todas las propiedades necesarias para los diferentes enfoques de mensajería:

```properties
spring.application.name=kafkapoc
server.port=8080

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Spring Cloud Stream Binder Configuration
spring.cloud.stream.default-binder=kafka
spring.cloud.stream.binders.kafka.type=kafka
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.brokers=localhost:29092
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.auto-create-topics=true

spring.cloud.stream.binders.rabbit.type=rabbit
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.host=localhost
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.port=5672
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.username=guest
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.password=guest

# Spring Cloud Stream Kafka Bindings
spring.cloud.stream.bindings.process-in-0.destination=cloud-stream-topic
spring.cloud.stream.bindings.process-out-0.destination=cloud-stream-topic-processed
spring.cloud.stream.bindings.process-in-0.binder=kafka
spring.cloud.stream.bindings.process-out-0.binder=kafka

# Spring Cloud Stream RabbitMQ Bindings
spring.cloud.stream.bindings.processRabbit-in-0.destination=rabbit-input-queue
spring.cloud.stream.bindings.processRabbit-out-0.destination=rabbit-output-queue
spring.cloud.stream.bindings.processRabbit-in-0.binder=rabbit
spring.cloud.stream.bindings.processRabbit-out-0.binder=rabbit

# Kafka Streams Configuration
spring.cloud.stream.kafka.streams.binder.configuration.default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
spring.cloud.stream.kafka.streams.binder.configuration.default.value.serde=org.apache.kafka.common.serialization.Serdes$StringSerde

# Kafka Streams Bindings
spring.cloud.stream.bindings.kstream-in-0.destination=stream-input
spring.cloud.stream.bindings.kstream-out-0.destination=stream-output
spring.cloud.stream.bindings.kstream-in-0.binder=kafka
spring.cloud.stream.bindings.kstream-out-0.binder=kafka
```

### Docker Compose para Infraestructura

Para facilitar la ejecución de Kafka y RabbitMQ localmente, se proporciona un archivo `docker-compose.yml`:

```yaml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    hostname: kafka
    container_name: kafka
    ports:
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:29093
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092

  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

## Testing

El testing de aplicaciones que utilizan Kafka y RabbitMQ es un aspecto crucial del desarrollo. En esta sección, veremos cómo probar los diferentes componentes de nuestra aplicación.

### 1. Configuración de Testing

Para facilitar el testing, utilizamos `EmbeddedKafkaBroker` que proporciona un broker Kafka embebido para pruebas:

```java
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka());
        return new DefaultKafkaProducerFactory<>(producerProps);
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaBroker(1)
                .brokerProperty("listeners", "PLAINTEXT://localhost:9092")
                .brokerProperty("port", "9092");
    }
}
```

Esta configuración:
- Crea un broker Kafka embebido para pruebas
- Configura un `KafkaTemplate` para enviar mensajes al broker embebido
- Utiliza `KafkaTestUtils` para configurar las propiedades del productor

### 2. Test de Integración

Para probar la integración de todos los componentes, utilizamos `@SpringBootTest` con la configuración de test:

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
class KafkapocIntegrationTest {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se carga correctamente
    }
}
```

### 3. Testing de Consumidores Kafka

Para probar los consumidores Kafka, utilizamos mocks para simular la recepción de mensajes:

```java
@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        kafkaConsumerService = new KafkaConsumerService(objectMapper);
    }

    @Test
    void consumeMessage_shouldDeserializeAndProcessMessage() throws JsonProcessingException {
        // Given
        String messageJson = "{\"id\":\"test-id\",\"content\":\"Test message\",\"sender\":\"test-sender\",\"timestamp\":\"2023-01-01T12:00:00\",\"type\":\"SIMPLE\"}";
        Message message = Message.builder()
                .id("test-id")
                .content("Test message")
                .sender("test-sender")
                .timestamp(LocalDateTime.of(2023, 1, 1, 12, 0, 0))
                .type(Message.MessageType.SIMPLE)
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        when(jsonNode.isObject()).thenReturn(true);
        when(objectMapper.readTree(messageJson)).thenReturn(jsonNode);
        when(objectMapper.readValue(messageJson, Message.class)).thenReturn(message);

        // When
        kafkaConsumerService.consumeMessage(messageJson);

        // Then
        verify(objectMapper).readTree(messageJson);
        verify(objectMapper).readValue(messageJson, Message.class);
    }
}
```

### 4. Testing de Productores Kafka

Para probar los productores Kafka, utilizamos mocks para verificar que los mensajes se envían correctamente:

```java
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        kafkaProducerService = new KafkaProducerService(kafkaTemplate, objectMapper);
    }

    @Test
    void sendMessage_shouldSerializeAndSendMessage() throws JsonProcessingException {
        // Given
        String content = "Test message";
        String messageJson = "{\"id\":\"test-id\",\"content\":\"Test message\"}";

        // Mock UUID generation
        try (MockedStatic<UUID> mockedUUID = mockStatic(UUID.class)) {
            UUID mockUUID = mock(UUID.class);
            when(mockUUID.toString()).thenReturn("test-id");
            mockedUUID.when(UUID::randomUUID).thenReturn(mockUUID);

            // Mock serialization
            when(objectMapper.writeValueAsString(any(Message.class))).thenReturn(messageJson);

            // When
            Message result = kafkaProducerService.sendMessage(content);

            // Then
            verify(kafkaTemplate).send(eq("simple-messages"), eq("test-id"), eq(messageJson));
            assertEquals("Test message", result.getContent());
        }
    }
}
```

### 5. Testing de Cloud Stream

Para probar los procesadores de Cloud Stream, podemos probar directamente la función sin necesidad de un broker:

```java
class KafkaCloudStreamProcessorTest {

    private KafkaCloudStreamProcessor processor;
    private Function<Message, Message> processFunction;

    @BeforeEach
    void setUp() {
        processor = new KafkaCloudStreamProcessor();
        processFunction = processor.process();
    }

    @Test
    void process_shouldTransformMessage() {
        // Given
        String originalContent = "Test message";
        Message inputMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(originalContent)
                .sender("Test Sender")
                .timestamp(LocalDateTime.now())
                .type(Message.MessageType.SIMPLE)
                .build();

        // When
        Message result = processFunction.apply(inputMessage);

        // Then
        assertNotNull(result);
        assertNotEquals(inputMessage.getId(), result.getId());
        assertEquals("Processed by Spring Cloud Stream (Kafka): " + originalContent, result.getContent());
        assertEquals("Spring Cloud Stream Processor", result.getSender());
        assertNotNull(result.getTimestamp());
        assertEquals(Message.MessageType.PROCESSED, result.getType());
    }
}
```

### 6. application-test.properties

Para las pruebas, utilizamos una configuración específica que desactiva la autoconfiguración de Kafka y RabbitMQ:

```properties
# Test configuration for Kafka and RabbitMQ
# Disable auto-configuration for Kafka and RabbitMQ
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration

# Disable Cloud Stream auto-start
spring.cloud.stream.bindings.process-in-0.consumer.auto-startup=false
spring.cloud.stream.bindings.processRabbit-in-0.consumer.auto-startup=false
spring.cloud.stream.bindings.kstream-in-0.consumer.auto-startup=false

# Disable Kafka listener auto-startup
spring.kafka.listener.auto-startup=false

# Use in-memory mode for tests
spring.kafka.bootstrap-servers=localhost:9092
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

---

## Mejores Prácticas

### 1. Manejo de Errores

El manejo adecuado de errores es crucial en sistemas de mensajería. A continuación se muestra un ejemplo de cómo implementar un manejo robusto de errores en un consumidor Kafka:

```java
@KafkaListener(topics = "simple-messages", groupId = "simple-consumer-group")
public void consumeMessage(String messageJson) {
    try {
        Message message = objectMapper.readValue(messageJson, Message.class);
        processMessage(message);
    } catch (JsonProcessingException e) {
        log.error("Error deserializing message: {}", e.getMessage(), e);
        // Enviar a Dead Letter Queue
        sendToDeadLetterQueue(messageJson, e);
    } catch (Exception e) {
        log.error("Error processing message: {}", e.getMessage(), e);
        // Reintento con backoff exponencial
        scheduleRetry(messageJson, e);
    }
}
```

### 2. Configuración de Productores

La configuración adecuada de los productores Kafka es esencial para garantizar rendimiento y confiabilidad:

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Configuraciones de rendimiento
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Configuraciones de confiabilidad
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

**Explicación de las propiedades clave:**

- **BATCH_SIZE_CONFIG**: Tamaño del lote de mensajes para enviar en una sola solicitud.
- **LINGER_MS_CONFIG**: Tiempo de espera para acumular mensajes antes de enviar.
- **BUFFER_MEMORY_CONFIG**: Memoria total disponible para el buffer del productor.
- **ACKS_CONFIG**: "all" garantiza que todos los replicas confirmen la recepción.
- **RETRIES_CONFIG**: Número de reintentos en caso de fallo.
- **ENABLE_IDEMPOTENCE_CONFIG**: Evita duplicados en caso de reintentos.

### 3. Monitoreo y Métricas

El monitoreo es crucial para sistemas de mensajería. Spring Kafka proporciona eventos que pueden ser capturados para monitoreo:

```java
@Component
@Slf4j
public class KafkaMetrics {

    @EventListener
    public void handleSuccess(ConsumerRecordRecoveredEvent event) {
        log.info("Message recovered successfully: {}", event.getRecord().value());
    }

    @EventListener
    public void handleFailure(ConsumerRecordFailedEvent event) {
        log.error("Message processing failed: {}", event.getException().getMessage());
    }
}
```

---

## Configuración y Propiedades

### Archivo application.properties completo

```properties
spring.application.name=kafkapoc
server.port=8080

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Spring Cloud Stream Binder Configuration
spring.cloud.stream.default-binder=kafka
spring.cloud.stream.binders.kafka.type=kafka
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.brokers=localhost:29092
spring.cloud.stream.binders.kafka.environment.spring.cloud.stream.kafka.binder.auto-create-topics=true

spring.cloud.stream.binders.rabbit.type=rabbit
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.host=localhost
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.port=5672
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.username=guest
spring.cloud.stream.binders.rabbit.environment.spring.rabbitmq.password=guest

# Spring Cloud Stream Kafka Bindings
spring.cloud.stream.bindings.process-in-0.destination=cloud-stream-topic
spring.cloud.stream.bindings.process-out-0.destination=cloud-stream-topic-processed
spring.cloud.stream.bindings.process-in-0.binder=kafka
spring.cloud.stream.bindings.process-out-0.binder=kafka

# Spring Cloud Stream RabbitMQ Bindings
spring.cloud.stream.bindings.processRabbit-in-0.destination=rabbit-input-queue
spring.cloud.stream.bindings.processRabbit-out-0.destination=rabbit-output-queue
spring.cloud.stream.bindings.processRabbit-in-0.binder=rabbit
spring.cloud.stream.bindings.processRabbit-out-0.binder=rabbit

# Kafka Streams Configuration
spring.cloud.stream.kafka.streams.binder.configuration.default.key.serde=org.apache.kafka.common.serialization.Serdes$StringSerde
spring.cloud.stream.kafka.streams.binder.configuration.default.value.serde=org.apache.kafka.common.serialization.Serdes$StringSerde

# Kafka Streams Bindings
spring.cloud.stream.bindings.kstream-in-0.destination=stream-input
spring.cloud.stream.bindings.kstream-out-0.destination=stream-output
spring.cloud.stream.bindings.kstream-in-0.binder=kafka
spring.cloud.stream.bindings.kstream-out-0.binder=kafka
```

### Docker Compose para Infraestructura

```yaml
version: '3.8'

services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    hostname: kafka
    container_name: kafka
    ports:
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:29093
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092

  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

---

## Testing

### 1. Configuración de Testing

```java
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka());
        return new DefaultKafkaProducerFactory<>(producerProps);
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafka() {
        return new EmbeddedKafkaBroker(1)
                .brokerProperty("listeners", "PLAINTEXT://localhost:9092")
                .brokerProperty("port", "9092");
    }
}
```

### 2. Test de Integración

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
class KafkapocIntegrationTest {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se carga correctamente
    }
}
```

### 3. application-test.properties

```properties
# Test configuration for Kafka and RabbitMQ
# Disable auto-configuration for Kafka and RabbitMQ
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration

# Disable Cloud Stream auto-start
spring.cloud.stream.bindings.process-in-0.consumer.auto-startup=false
spring.cloud.stream.bindings.processRabbit-in-0.consumer.auto-startup=false
spring.cloud.stream.bindings.kstream-in-0.consumer.auto-startup=false

# Disable Kafka listener auto-startup
spring.kafka.listener.auto-startup=false

# Use in-memory mode for tests
spring.kafka.bootstrap-servers=localhost:9092
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

---

## Mejores Prácticas

### 1. Manejo de Errores

```java
@KafkaListener(topics = "simple-messages", groupId = "simple-consumer-group")
public void consumeMessage(String messageJson) {
    try {
        Message message = objectMapper.readValue(messageJson, Message.class);
        processMessage(message);
    } catch (JsonProcessingException e) {
        log.error("Error deserializing message: {}", e.getMessage(), e);
        // Enviar a Dead Letter Queue
        sendToDeadLetterQueue(messageJson, e);
    } catch (Exception e) {
        log.error("Error processing message: {}", e.getMessage(), e);
        // Reintento con backoff exponencial
        scheduleRetry(messageJson, e);
    }
}
```

### 2. Configuración de Productores

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Configuraciones de rendimiento
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Configuraciones de confiabilidad
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

### 3. Monitoreo y Métricas

```java
@Component
@Slf4j
public class KafkaMetrics {

    @EventListener
    public void handleSuccess(ConsumerRecordRecoveredEvent event) {
        log.info("Message recovered successfully: {}", event.getRecord().value());
    }

    @EventListener
    public void handleFailure(ConsumerRecordFailedEvent event) {
        log.error("Message processing failed: {}", event.getException().getMessage());
    }
}
```

---

## Comparación de Enfoques

| Aspecto | Spring Kafka | Kafka Streams | Cloud Stream | RabbitMQ |
|---------|--------------|---------------|--------------|----------|
| **Complejidad** | Media | Alta | Baja | Baja |
| **Flexibilidad** | Alta | Muy Alta | Media | Media |
| **Rendimiento** | Alto | Muy Alto | Alto | Medio |
| **Abstracción** | Baja | Media | Alta | Alta |
| **Casos de uso** | Pub/Sub simple | Stream processing | Microservicios | RPC, transacciones |
| **Curva de aprendizaje** | Media | Alta | Baja | Baja |

### Cuándo usar cada uno:

- **Spring Kafka**: Control fino, alta performance, casos complejos
- **Kafka Streams**: Procesamiento de flujos, agregaciones, joins
- **Cloud Stream**: Prototipado rápido, abstracción, cambio de brokers
- **RabbitMQ**: Transacciones, patrones complejos de routing, baja latencia

---

## Conclusiones

Este proyecto demuestra la **evolución de la mensajería en Spring**:

1. **Spring Kafka** ofrece control granular
2. **Kafka Streams** permite procesamiento complejo
3. **Cloud Stream** proporciona abstracción y flexibilidad
4. **RabbitMQ** aporta diferentes patrones de mensajería

La elección del enfoque depende de:
- **Requisitos de performance**
- **Complejidad del procesamiento**
- **Necesidades de abstracción**
- **Experiencia del equipo**

### Próximos pasos:

1. Implementar Dead Letter Queues
2. Añadir métricas y monitoreo
3. Configurar múltiples particiones
4. Implementar Saga patterns
5. Añadir testing con TestContainers

---

## Ejemplos de Curl para Probar los Endpoints

A continuación, se presentan ejemplos de comandos curl para probar los diferentes endpoints de la aplicación:

### 1. Enviar mensaje usando Spring Kafka

```bash
curl -X POST http://localhost:8080/api/messages/kafka \
  -H "Content-Type: application/json" \
  -d '{"content": "Mensaje de prueba para Spring Kafka"}'
```

### 2. Enviar mensaje a Kafka Streams

```bash
curl -X POST http://localhost:8080/api/messages/kafka-streams \
  -H "Content-Type: application/json" \
  -d '{"content": "Mensaje de prueba para Kafka Streams"}'
```

### 3. Enviar mensaje usando Spring Cloud Stream con Kafka

```bash
curl -X POST http://localhost:8080/api/messages/cloud-stream-kafka \
  -H "Content-Type: application/json" \
  -d '{"content": "Mensaje de prueba para Spring Cloud Stream con Kafka"}'
```

### Respuesta esperada

Todos los endpoints devuelven un objeto Message con la siguiente estructura:

```json
{
  "id": "uuid-generado-automáticamente",
  "content": "El contenido del mensaje enviado",
  "sender": "REST API",
  "timestamp": "2023-05-15T14:30:45.123",
  "type": "SIMPLE"
}
```

---

## Documentación OpenAPI

### Creación manual de especificación OpenAPI

Aunque no hemos podido añadir las dependencias de SpringDoc OpenAPI directamente al proyecto, puedes crear manualmente un archivo de especificación OpenAPI (anteriormente conocido como Swagger) para documentar los endpoints de la API.

A continuación se muestra un ejemplo de cómo sería la especificación OpenAPI 3.0 para esta aplicación:

```yaml
openapi: 3.0.3
info:
  title: API de Mensajería con Kafka
  description: API para enviar mensajes a diferentes sistemas de mensajería (Kafka, Kafka Streams, Spring Cloud Stream)
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Servidor de desarrollo local
paths:
  /api/messages/kafka:
    post:
      summary: Enviar mensaje usando Spring Kafka
      description: Envía un mensaje utilizando la implementación directa de Spring Kafka
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MessageRequest'
      responses:
        '200':
          description: Mensaje enviado correctamente
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message'
  /api/messages/kafka-streams:
    post:
      summary: Enviar mensaje a Kafka Streams
      description: Envía un mensaje para ser procesado por Kafka Streams
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MessageRequest'
      responses:
        '200':
          description: Mensaje enviado correctamente
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message'
  /api/messages/cloud-stream-kafka:
    post:
      summary: Enviar mensaje usando Spring Cloud Stream con Kafka
      description: Envía un mensaje utilizando Spring Cloud Stream con Kafka como broker
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MessageRequest'
      responses:
        '200':
          description: Mensaje enviado correctamente
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message'
components:
  schemas:
    MessageRequest:
      type: object
      required:
        - content
      properties:
        content:
          type: string
          description: Contenido del mensaje a enviar
    Message:
      type: object
      properties:
        id:
          type: string
          format: uuid
          description: Identificador único del mensaje
        content:
          type: string
          description: Contenido del mensaje
        sender:
          type: string
          description: Remitente del mensaje
        timestamp:
          type: string
          format: date-time
          description: Fecha y hora de creación del mensaje
        type:
          type: string
          enum: [SIMPLE, PROCESSED, STREAMED]
          description: Tipo de mensaje
```

### Uso de la especificación OpenAPI

Para utilizar esta especificación:

1. Guarda el contenido YAML anterior en un archivo llamado `openapi.yaml`
2. Utiliza herramientas como [Swagger Editor](https://editor.swagger.io/) para visualizar y probar la API
3. Importa el archivo en herramientas como Postman para generar colecciones de pruebas

### Integración con Swagger UI (para futuras implementaciones)

Para integrar Swagger UI en el proyecto, necesitarías:

1. Añadir las dependencias de SpringDoc OpenAPI al pom.xml:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.1.0</version>
   </dependency>
   ```

2. Configurar la documentación OpenAPI en una clase de configuración:
   ```java
   @Configuration
   public class OpenApiConfig {
       @Bean
       public OpenAPI customOpenAPI() {
           return new OpenAPI()
                   .info(new Info()
                           .title("API de Mensajería con Kafka")
                           .description("API para enviar mensajes a diferentes sistemas de mensajería")
                           .version("1.0.0"));
       }
   }
   ```

3. Acceder a la UI de Swagger en: http://localhost:8080/swagger-ui.html
