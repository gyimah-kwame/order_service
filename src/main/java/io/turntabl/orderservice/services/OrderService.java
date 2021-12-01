package io.turntabl.orderservice.services;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface OrderService {

    OrderDto createOrder(String userId, OrderRequest orderRequest);

    OrderDto updateOrder(String id, String userId, OrderRequest orderRequest);

    Boolean deleteOrder(String id, String userId);

    List<OrderDto> getAllOrders(String userId);
}
