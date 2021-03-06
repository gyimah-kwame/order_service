package io.turntabl.orderservice.dtos;

import io.turntabl.orderservice.enums.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderInformationDto {

    private String exchangeUrl;

    private String orderId;

    private int quantity;

    private int quantityFulfilled;

    private OrderItemStatus status;

    private double price;

}
