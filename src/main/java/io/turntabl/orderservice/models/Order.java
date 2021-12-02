package io.turntabl.orderservice.models;

import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document("orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @Field(name = "order_id")
    private String orderId;

    @Field(name = "user_id")
    private String userId;

    private Side side;

    private Double price;

    private int quantity;

    private String ticker;

    private OrderStatus status;

    @Field(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static Order fromDto(OrderDto dto){
        Order order = new Order();

        order.setSide(dto.getSide());
        order.setOrderId(dto.getOrderId());
        order.setPrice(dto.getPrice());
        order.setQuantity(dto.getQuantity());
        order.setTicker(dto.getTicker());
        order.setId(dto.getId());
        order.setUserId(dto.getUserId());
        order.setStatus(dto.getStatus());


        return order;
    }

}

