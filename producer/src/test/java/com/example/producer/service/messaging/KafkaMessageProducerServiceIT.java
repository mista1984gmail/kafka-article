package com.example.producer.service.messaging;

import com.example.producer.service.messaging.event.OrderSendEvent;
import com.example.producer.service.messaging.service.KafkaMessagingService;
import com.example.producer.service.messaging.util.FakeOrder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class KafkaMessageProducerServiceIT {
    public static final String TOPIC_NAME_SEND_CLIENT = "send-order-event";
    @Autowired
    private KafkaMessagingService kafkaMessagingService;

    @Test
    public void it_should_send_order_event() {
        OrderSendEvent order = FakeOrder.getOrderSendEvent();
        kafkaMessagingService.sendOrder(order);

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group-java-test");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderSendEvent.class);
        KafkaConsumer<String, OrderSendEvent> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(TOPIC_NAME_SEND_CLIENT));
        ConsumerRecords<String, OrderSendEvent> records = consumer.poll(Duration.ofMillis(10000L));
        consumer.close();

        //then
        assertEquals(1, records.count());
        assertEquals(order.getProductName(), records.iterator().next().value().getProductName());
        assertEquals(order.getBarCode(), records.iterator().next().value().getBarCode());
        assertEquals(order.getQuantity(), records.iterator().next().value().getQuantity());
        assertEquals(order.getPrice(), records.iterator().next().value().getPrice());
    }
}
