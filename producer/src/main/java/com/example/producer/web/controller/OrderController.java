package com.example.producer.web.controller;

import com.example.producer.model.Order;
import com.example.producer.service.messaging.producer.Producer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final Producer producer;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Order sendOrder(@RequestBody Order order) {
        log.info("Send order to kafka");
        producer.sendOrderEvent(order);
        return order;
    }

}
