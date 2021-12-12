package io.turntabl.orderservice.exceptions;

public class InsufficientWalletBalanceException extends RuntimeException{
    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}
