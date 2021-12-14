package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.exceptions.OrderNotFoundException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.RedisService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@DisplayName("Order Service Tests")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {


    @Mock
    OrderRepository orderRepository;
    @Mock
    RedisService redisService;

    @InjectMocks
    OrderServiceImpl orderService;


    @Test
    @DisplayName("Creating New Order Test")
    void createOrder() {

        Mockito.when(orderRepository.save(ArgumentMatchers.any()))
                .thenReturn(new Order("124", new ArrayList<>(),"1234", Side.SELL,12.0,100,0,"AMZ", OrderStatus.PENDING, "",LocalDateTime.now(), LocalDateTime.now()));
        OrderDto order = orderService.createOrder(ArgumentMatchers.anyString(), OrderDto.fromRequest(new OrderRequest("sell",1.0,120,"AMZ")));

        Assertions.assertThat("AMZ")
                .isEqualTo(order.getTicker());

    }

    @Test
    @DisplayName("Test Update Order")
    void updateOrder() {
        Mockito.when(orderRepository
                        .findByIdAndUserIdOrderByCreatedAtDesc(ArgumentMatchers.anyString(),ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new Order("124", new ArrayList<>(),"1234", Side.SELL,12.0,100,0,"AMZ", OrderStatus.PENDING, "",LocalDateTime.now(), LocalDateTime.now())));

        Mockito.when(orderRepository.save(ArgumentMatchers.any()))
                .thenReturn(new Order("124", new ArrayList<>(),"1234", Side.SELL,12.0,100,0,"AMZ", OrderStatus.PENDING, "",LocalDateTime.now(), LocalDateTime.now()));

        Assertions.assertThat(12.00)
                .isEqualTo(orderService.updateOrder(ArgumentMatchers.anyString(),ArgumentMatchers.anyString(),OrderDto.fromRequest(new OrderRequest("sell",1.0,120,"AMZ"))).getPrice());
    }

    @Test
    @DisplayName("Expect Exception when Order is not found")
    void updateOrderException() {
        Mockito.when(orderRepository
                        .findByIdAndUserIdOrderByCreatedAtDesc(ArgumentMatchers.anyString(),ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.updateOrder("OrderID","userID",OrderDto.fromRequest(new OrderRequest("sell",1.0,120,"AMZ"))))
                .withMessage("order with id OrderID does not exists");


    }

}
