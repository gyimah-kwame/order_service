package io.turntabl.orderservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ControllerAdvisor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<Object> handleInvalidOrder(InvalidOrderException e, WebRequest request){
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "order is invalid");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

}
