package io.turntabl.orderservice.exceptions;

public class WalletNotFoundException extends RuntimeException{
    public WalletNotFoundException(String description) {
        super(description);
    }
}
