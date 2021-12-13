package io.turntabl.orderservice.requests;

import io.turntabl.orderservice.models.Order;
import lombok.Data;

@Data
public class MalonOrderRequest {

    private String product;

    private int quantity;

    private double price;

    private String side;

    public static MalonOrderRequest fromOrder(Order order, double price, int quantity) {
        MalonOrderRequest malonOrderRequest = new MalonOrderRequest();
        malonOrderRequest.price = price;
        malonOrderRequest.product = order.getTicker();
        malonOrderRequest.side = order.getSide().toString();
        malonOrderRequest.quantity = quantity;

        return malonOrderRequest;
    }

}
