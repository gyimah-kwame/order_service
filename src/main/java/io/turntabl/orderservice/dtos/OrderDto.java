package io.turntabl.orderservice.dtos;

import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.requests.OrderRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The type Order dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private String id;

    private List<OrderInformationDto> orderInformation;

    private String userId;

    private Side side;

    private Double price;

    private int quantity;

    private String ticker;

    private OrderStatus status;

    private LocalDateTime createdAt;



    public static OrderDto fromRequest(OrderRequest orderRequest) {
        OrderDto orderDto = new OrderDto();
        orderDto.side = Side.valueOf(orderRequest.getSide().toUpperCase());
        orderDto.price = orderRequest.getPrice();
        orderDto.quantity = orderRequest.getQuantity();
        orderDto.ticker = orderRequest.getTicker();

        return orderDto;
    }

    public static OrderDto fromEntity(Order order){

        OrderDto orderDto = new OrderDto();

        orderDto.id = order.getId();
        orderDto.orderInformation = order.getOrderInformation();
        orderDto.quantity = order.getQuantity();
        orderDto.price = order.getPrice();
        orderDto.ticker = order.getTicker();
        orderDto.createdAt = order.getCreatedAt();
        orderDto.status = order.getStatus();
        orderDto.side = order.getSide();
        orderDto.userId = order.getUserId();

        return orderDto;
    }
}
