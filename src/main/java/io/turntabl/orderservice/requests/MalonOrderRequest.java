package io.turntabl.orderservice.requests;

import io.turntabl.orderservice.models.Order;
import lombok.Data;

@Data
public class MalonOrderRequest {

    private String product;

    private int quantity;

    private double price;

    private String side;

    public static MalonOrderRequest fromOrder(Order order) {
        MalonOrderRequest malonOrderRequest = new MalonOrderRequest();
        malonOrderRequest.price = order.getPrice();
        malonOrderRequest.product = order.getTicker();
        malonOrderRequest.side = order.getSide().toString();
        malonOrderRequest.quantity = order.getQuantity();

        return malonOrderRequest;
    }

}
