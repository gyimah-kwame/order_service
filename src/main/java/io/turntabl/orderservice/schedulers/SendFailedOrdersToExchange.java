package io.turntabl.orderservice.schedulers;

import io.turntabl.orderservice.dtos.ExchangeDto;
import io.turntabl.orderservice.dtos.OrderInformationDto;
import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.MalonOrderRequest;
import io.turntabl.orderservice.services.OrderService;
import io.turntabl.orderservice.services.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SendFailedOrdersToExchange {

    @Autowired
    private WebClient webClient;

    @Value("${matraining.exchange.one.token}")
    private String exchangeOneApiKey;
    @Value("${matraining.exchange.two.token}")
    private String exchangeTwoApiKey;


    @Autowired
    private RedisService redisService;

    @Autowired
    private OrderRepository orderRepository;


    @Scheduled(cron = "*/5 * * * * *")
    public void sendOpenOrders() {
        List<Order> orders = orderRepository.findByStatus(OrderStatus.OPEN.name());

        for(Order order : orders) {

            order.getOrderInformation()
                    .stream()
                    .filter(orderItem -> orderItem.getStatus().equals(OrderItemStatus.FAILED))
                    .forEach(item -> {
                        String orderId = sendOrderToExchange(order, item.getPrice(), item.getExchangeUrl(), item.getQuantity());

                       if (!orderId.equals("")) {
                           item.setOrderId(orderId);
                           item.setStatus(OrderItemStatus.PENDING);

                           orderRepository.save(order);
                       }

                    });

        }


    }

    @Scheduled(cron = "*/5 * * * * *")
    public void sendPendingOrders() {
        List<Order> orders = orderRepository.findByStatus(OrderStatus.PENDING.name());

        orders.forEach(order -> {
            redisService.convertAndSendToCreateOrderTopic(order.getId());
        });

    }


    private String sendOrderToExchange(Order order, double price, String exchangeUrl, int quantity) {

        MalonOrderRequest malonOrderRequest = new MalonOrderRequest();
        malonOrderRequest.setPrice(price);
        malonOrderRequest.setQuantity(quantity);
        malonOrderRequest.setProduct(order.getTicker());


        return webClient.post()
                .uri(exchangeUrl+"/"+ (exchangeUrl.equalsIgnoreCase("https://exchange.matraining.com") ? exchangeOneApiKey : exchangeTwoApiKey) +"/order")
                .body(Mono.just(malonOrderRequest), MalonOrderRequest.class)
                .retrieve()
                .bodyToMono(String.class)
                .map(received ->  received.substring(1, received.length()-1))
                .doOnError(throwable -> {
                    log.info("Processing Order Item failed");
                })
                .onErrorReturn("")
                .block();
    }
}
