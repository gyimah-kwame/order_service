package io.turntabl.orderservice.schedulers;

import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.exceptions.OrderStatusException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.responses.MalonOrderStatusResponse;
import io.turntabl.orderservice.responses.OrderStatusResponse;
import io.turntabl.orderservice.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class CheckOrderStatusScheduler {

    @Autowired
    private WebClient webClient;

    @Value("${matraining.exchange.one.token}")
    private String exchangeOneApiKey;

    @Value("${matraining.exchange.two.token}")
    private String exchangeTwoApiKey;

    @Autowired
    private OrderService orderService;


    @Autowired
    private OrderRepository orderRepository;

//    @Scheduled(cron = "*/2 * * * * *")
    public void checkOrderStatus() {

        List<Order> orders = orderRepository.findByStatus(OrderStatus.PROCESSING.toString());

        if (orders.size() == 0) return;

        for( Order order : orders) {

            order.getOrderInformation().forEach(item -> {
                webClient.get()
                        .uri(String.format("%s/%s/order/%s", item.getExchangeUrl(), exchangeOneApiKey, item.getOrderId()))
                        .retrieve()
                        .onStatus(HttpStatus::is5xxServerError, response -> response.bodyToMono(OrderStatusResponse.class)
                                .flatMap(error -> {
                                    handleOrderStatusException(error.getMessage(), order.getId(), item.getOrderId());
                                    return Mono.error(new OrderStatusException(error.getMessage(), order.getId(), item.getOrderId()));
                                }))
                        .bodyToMono(MalonOrderStatusResponse.class)
                        .subscribe(x -> log.info("success"));

            });

        }
    }

    public void handleOrderStatusException(String message, String mongoOrderId, String orderItemId){

        if (message.equalsIgnoreCase("The order ID provided does not match to a valid open order.")) {

            orderService.updateOrderStatus(mongoOrderId, orderItemId, OrderItemStatus.FULFILLED, -1);

        }

    }
}
