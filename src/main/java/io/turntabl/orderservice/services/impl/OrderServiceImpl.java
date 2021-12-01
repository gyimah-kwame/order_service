package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public OrderDto createOrder(String userId, OrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(userId);
        order.setSide(orderRequest.getSide());
        order.setQuantity(orderRequest.getQuantity());
        order.setTicker(orderRequest.getTicker());
        order.setPrice(orderRequest.getPrice());
        order.setStatus(OrderStatus.PENDING);

        OrderDto orderDto = OrderDto.fromModel(orderRepository.save(order));

        //:TODO PUSH TO A PARTICULAR TOPIC
        return orderDto;
    }

    @Override
    public OrderDto updateOrder(String id, String userId, OrderRequest orderRequest) {
        return null;
    }

    @Override
    public Boolean deleteOrder(String id, String userId) {
        return null;
    }

    @Override
    public List<OrderDto> getAllOrders(String userId) {
        return null;
    }
}
