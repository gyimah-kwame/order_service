package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RedisMessageListenerImpl implements MessageListener {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("{}", message.toString());

        Optional<Order> order = orderRepository.findById(message.toString());

        if (order.isEmpty()) return;

        // validation goes here

        Order receivedOrder = order.get();

        //perform limit checking

        //if side is buy, check if user has enough funds to place the order

        // if side is sell, check if user owns more or equal to the quantity of products being sold


    }
}
