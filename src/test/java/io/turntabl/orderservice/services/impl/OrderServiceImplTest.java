package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.Request;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Service Tests")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

//    @Mock
//    OrderRepository orderRepository;
//    @Mock
//    RedisTemplate redisTemplate;
//
//    @InjectMocks
//    OrderServiceImpl orderService;
//
//
//    @Test
//    void createOrder() {
//
//        Mockito.doNothing()
//                .when(redisTemplate)
//                .convertAndSend(ArgumentMatchers.anyString(),ArgumentMatchers.anyString());
//
//        Mockito.when(orderRepository.save(ArgumentMatchers.any()))
//                .thenReturn(new Order("124", new ArrayList<>(),"1234", Side.SELL,12.0,100,0,"AMZ", OrderStatus.PENDING, "",LocalDateTime.now(), LocalDateTime.now()));
//        OrderDto order = orderService.createOrder(ArgumentMatchers.anyString(), OrderDto.fromRequest(new OrderRequest("sell",1.0,120,"AMZ")));
//
//        Assertions.assertThat("AMZ")
//                .isEqualTo(order.getTicker());
//
//    }
//
//    @Test
//    void updateOrder() {
//    }
//
//    @Test
//    void findOrdersByStatus() {
//    }
//
//    @Test
//    void getUserOrdersByStatus() {
//    }
//
//    @Test
//    void deleteOrder() {
//    }
//
//    @Test
//    void getAllOrders() {
//    }
//
//    @Test
//    void updateOrderStatus() {
//    }
}
