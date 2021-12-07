package io.turntabl.orderservice.exceptions;

import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.responses.ValidationErrorResponse;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class ControllerAdvisor  extends ResponseEntityExceptionHandler{

    private final OrderService  orderService;

    @Override
    protected ResponseEntity<Object>  handleMethodArgumentNotValid( MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                    HttpStatus status, WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->  errors.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(new ValidationErrorResponse(HttpStatus.BAD_REQUEST.value(),LocalDateTime.now(), errors), HttpStatus.BAD_REQUEST);

    }

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
