package io.turntabl.orderservice.exceptions;

import lombok.Data;

@Data
public class OrderStatusException extends RuntimeException {

    private String orderId;
    private String mongoOrderId;

    public OrderStatusException(String message, String mongoOrderId, String orderId) {
      super(message);
      this.orderId = orderId;
      this.mongoOrderId = mongoOrderId;
    }

}
