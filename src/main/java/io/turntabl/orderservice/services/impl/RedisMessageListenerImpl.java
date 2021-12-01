package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.MarketDataDto;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RedisMessageListenerImpl implements MessageListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Gson gson;

    @Autowired
    private HashOperations<String, String, String> hashOperations;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("{}", message.toString());

        Optional<Order> order = orderRepository.findById(message.toString());

        if (order.isEmpty()) return;

        // validation goes here

        Order receivedOrder = order.get();

        String marketDataDtoStr = hashOperations.get(receivedOrder.getTicker(), receivedOrder.getTicker());

        MarketDataDto marketDataDto = gson.fromJson(marketDataDtoStr, MarketDataDto.class);

        log.info("data from hash {} ",marketDataDto);

        //perform limit checking on buy
        if (receivedOrder.getSide() == Side.BUY && receivedOrder.getQuantity() > marketDataDto.getBuyLimit()) {
            return;
        }

        if (receivedOrder.getSide() == Side.SELL && receivedOrder.getQuantity() > marketDataDto.getSellLimit()) {
            return;
        }

        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();

        if (receivedOrder.getSide() == Side.BUY) {

        }

        //if side is buy, check if user has enough funds to place the order

        // if side is sell, check if user owns more or equal to the quantity of products being sold


    }
}
