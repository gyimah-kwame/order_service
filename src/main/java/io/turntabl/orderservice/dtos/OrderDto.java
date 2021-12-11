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
        orderDto.setSide(Side.valueOf(orderRequest.getSide().toUpperCase()));
        orderDto.setPrice(orderRequest.getPrice());
        orderDto.setQuantity(orderRequest.getQuantity());
        orderDto.setTicker(orderRequest.getTicker());
        return orderDto;
    }

    public static OrderDto fromEntity(Order order){
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setOrderInformation(order.getOrderInformation());
        orderDto.setQuantity(order.getQuantity());
        orderDto.setPrice(order.getPrice());
        orderDto.setTicker(order.getTicker());
        orderDto.setPrice(order.getPrice());
        orderDto.setCreatedAt(order.getCreatedAt());
        orderDto.setStatus(order.getStatus());
        orderDto.setSide(order.getSide());
        orderDto.setUserId(order.getUserId());

        return orderDto;
    }
}
