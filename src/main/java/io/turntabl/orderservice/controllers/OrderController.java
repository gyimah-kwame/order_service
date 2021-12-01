package io.turntabl.orderservice.controllers;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @ResponseStatus(code = HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody OrderRequest orderRequest){
        return orderService.createOrder("1", orderRequest);
    }

    @GetMapping("/orders")
    public List<OrderDto> getOrders(){
        return orderService.getAllOrders("1");
    }



}
