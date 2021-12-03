package io.turntabl.orderservice.services;

import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface OrderService {

    OrderDto createOrder(String userId, OrderRequest orderRequest);

    OrderDto updateOrder(String id, String userId, OrderRequest orderRequest);

    List<OrderDto> findOrdersByStatus(String status);

    Boolean deleteOrder(String id, String userId);


    void updateOrderStatus(String orderId, String orderItemId, OrderItemStatus status, int quantityFulfilled);

    List<OrderDto> getAllOrders(String userId);
}
