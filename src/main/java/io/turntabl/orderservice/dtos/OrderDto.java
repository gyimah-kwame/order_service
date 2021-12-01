package io.turntabl.orderservice.dtos;

import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.models.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private String id;

    private String orderId;

    private String userId;

    private Side side;

    private Double price;

    private int quantity;

    private String ticker;

    private OrderStatus status;

    private LocalDateTime createdAt;



    public static OrderDto fromModel(Order order){
        OrderDto orderDto = new OrderDto();

//        BeanUtils.copyProperties(order, orderDto);

        orderDto.setId(order.getId());
        orderDto.setOrderId(order.getOrderId());
        orderDto.setQuantity(order.getQuantity());
        orderDto.setPrice(order.getPrice());
        orderDto.setTicker(order.getTicker());
        orderDto.setPrice(order.getPrice());
        orderDto.setCreatedAt(order.getCreatedAt());
        orderDto.setStatus(order.getStatus());
        orderDto.setSide(order.getSide());

        return orderDto;
    }
}
