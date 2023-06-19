package com.example.consumer.service.messaging.service;

import com.example.consumer.domain.Order;
import com.example.consumer.domain.repository.OrdersRepository;
import com.example.consumer.service.messaging.event.OrderEvent;
import com.example.consumer.service.messaging.service.util.FakeOrder;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import scala.concurrent.duration.Duration;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class KafkaMessagingServiceIT {
    public static final Long ORDER_ID = 1L;
    public static final String TOPIC_NAME_SEND_ORDER= "send-order-event";

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .withUsername("username")
            .withPassword("password")
            .withExposedPorts(5432)
            .withReuse(true);
    @Container
    static final KafkaContainer kafkaContainer =
                new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.4"))
            .withEmbeddedZookeeper()
          .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093 ,BROKER://0.0.0.0:9092")
          .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
          .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
          .withEnv("KAFKA_BROKER_ID", "1")
          .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
          .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
          .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
          .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
          .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "")
          .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");


    static {
        Startables.deepStart(Stream.of(postgreSQLContainer, kafkaContainer)).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
    }

    @Autowired
    private OrdersRepository ordersRepository;

    @Test
    void save_order() throws InterruptedException {
        //given
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        OrderEvent orderEvent = FakeOrder.getOrderEvent();
        Order order = FakeOrder.getOrder();

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, OrderEvent> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        KafkaTemplate<String, OrderEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        //when

        SECONDS.sleep(5);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, orderEvent.getBarCode(), orderEvent);
        SECONDS.sleep(5);

        //then
        Order orderFromDB = ordersRepository.findById(ORDER_ID).get();
        assertEquals(orderFromDB.getId(), ORDER_ID);
        assertEquals(orderFromDB.getProductName(), order.getProductName());
        assertEquals(orderFromDB.getBarCode(), order.getBarCode());
        assertEquals(orderFromDB.getQuantity(), order.getQuantity());
        assertEquals(orderFromDB.getPrice(), order.getPrice().setScale(2,  RoundingMode.HALF_DOWN));
        assertEquals(orderFromDB.getAmount(), order.getAmount().setScale(2));
        assertEquals(orderFromDB.getOrderDate().getYear(), order.getOrderDate().getYear());
        assertEquals(orderFromDB.getStatus(), order.getStatus());
    }
}