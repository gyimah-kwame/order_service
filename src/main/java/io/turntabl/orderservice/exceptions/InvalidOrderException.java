package io.turntabl.orderservice.exceptions;

public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String id) {
        super("Order with id "+ id + " is not valid");
    }
}
