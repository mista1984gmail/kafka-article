package com.example.consumer.service.messaging.service;

import com.example.consumer.service.OrderService;
import com.example.consumer.service.dto.OrderDto;
import com.example.consumer.service.messaging.event.OrderEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Slf4j
@Service
@AllArgsConstructor
public class KafkaMessagingService {
    private static final String topicCreateOrder = "${topic.send-order}";
    private static final String kafkaConsumerGroupId = "${spring.kafka.consumer.group-id}";
    private final OrderService orderService;
    private final ModelMapper modelMapper;

    @Transactional
    @KafkaListener(topics = topicCreateOrder, groupId = kafkaConsumerGroupId, properties = {"spring.json.value.default.type=com.example.consumer.service.messaging.event.OrderEvent"})
    public OrderEvent createOrder(OrderEvent orderEvent) {
        log.info("Message consumed {}", orderEvent);
        orderService.save(modelMapper.map(orderEvent, OrderDto.class));
        return orderEvent;
    }

}
