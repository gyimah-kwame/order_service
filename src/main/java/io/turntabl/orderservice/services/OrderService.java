package io.turntabl.orderservice.services;

import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The interface Order service.
 */
@Component
public interface OrderService {

    /**
     * Create order dto.
     *
     * @param userId   the user id
     * @param orderDto the order dto
     * @return the order dto
     */
    OrderDto createOrder(String userId, OrderDto orderDto);

    /**
     * Update order order dto.
     *
     * @param id         the id
     * @param userId     the user id
     * @param requestDto the request dto
     * @return the order dto
     */
    OrderDto updateOrder(String id, String userId, OrderDto requestDto);

    /**
     * Find orders by status list.
     *
     * @param status the status
     * @return the list
     */
    List<OrderDto> findOrdersByStatus(String status);

    /**
     * Gets user orders by status.
     *
     * @param userId the user id
     * @param status the status
     * @return the user orders by status
     */
    List<OrderDto> getUserOrdersByStatus(String userId, String status);

    /**
     * Delete order.
     *
     * @param id     the id
     * @param userId the user id
     */
    void deleteOrder(String id, String userId);

    /**
     * Update order status.
     *
     * @param orderId           the order id
     * @param orderItemId       the order item id
     * @param status            the status
     * @param quantityFulfilled the quantity fulfilled
     */
    void updateOrderStatus(String orderId, String orderItemId, OrderItemStatus status, int quantityFulfilled);

    /**
     * Gets all orders.
     *
     * @param userId the user id
     * @return the all orders
     */
    List<OrderDto> getAllOrders(String userId);

//    OrderDto findById(String orderId);

}
