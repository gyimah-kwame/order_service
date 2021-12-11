package io.turntabl.orderservice.services;

import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface OrderService {

    OrderDto createOrder(String userId, OrderDto orderDto);

    OrderDto updateOrder(String id, String userId, OrderDto requestDto);

    List<OrderDto> findOrdersByStatus(String status);

    List<OrderDto> getUserOrdersByStatus(String userId, String status);

    void deleteOrder(String id, String userId);

    void updateOrderStatus(String orderId, String orderItemId, OrderItemStatus status, int quantityFulfilled);

    List<OrderDto> getAllOrders(String userId);

//    OrderDto findById(String orderId);

}
