package io.turntabl.orderservice.schedulers;

import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Slf4j
public class SendPendingOrdersToExchange {

    @Autowired
    private WebClient webClient;

    @Value("${matraining.exchange.one.token}")
    private String exchangeOneApiKey;
    @Value("${matraining.exchange.two.token}")
    private String exchangeTwoApiKey;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Qualifier("createOrderTopic")
    @Autowired
    private ChannelTopic topic;


//    @Scheduled(cron = "*/1 * * * * *")
    public void sendPendingOrders() {
        List<OrderDto> orders = orderService.findOrdersByStatus(OrderStatus.PENDING.toString());

        orders.forEach(order -> {
            stringRedisTemplate.convertAndSend(topic.getTopic(), order.getId());
        });
    }
}
