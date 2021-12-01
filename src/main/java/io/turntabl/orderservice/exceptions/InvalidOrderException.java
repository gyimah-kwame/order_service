package io.turntabl.orderservice.exceptions;

public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String id) {
        super("Order with id "+ " is not valid");
    }
}
