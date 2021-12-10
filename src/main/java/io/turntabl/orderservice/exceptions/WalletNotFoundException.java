package io.turntabl.orderservice.exceptions;

public class WalletNotFoundException extends RuntimeException{
    private String orderId;

    public WalletNotFoundException(String description, String orderId) {
        super(description);
        this.orderId = orderId;
    }
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
