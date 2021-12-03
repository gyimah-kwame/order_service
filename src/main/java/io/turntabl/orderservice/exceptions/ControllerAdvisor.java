package io.turntabl.orderservice.exceptions;

import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class ControllerAdvisor {

    private final OrderService  orderService;

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<Object> handleInvalidOrder(InvalidOrderException e, WebRequest request){
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "order is invalid");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderStatusException.class)
    public void handleOrderStatusException(OrderStatusException e){

        log.info("updating");

        log.info(e.getMessage());
        log.info(e.getLocalizedMessage());

        if (e.getMessage().equalsIgnoreCase("The order ID provided does not match to a valid open order.")) {

            log.info("updating");

            String mongoOrderId = e.getMongoOrderId();

            String orderId = e.getOrderId();

            orderService.updateOrderStatus(mongoOrderId, orderId, OrderItemStatus.FULFILLED, -1);

        }

    }

}
