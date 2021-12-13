package io.turntabl.orderservice.controllers;

import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin
@AllArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @ResponseStatus(code = HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody OrderRequest orderRequest, @AuthenticationPrincipal Jwt principal) {
        return orderService.createOrder(principal.getSubject(), OrderDto.fromRequest(orderRequest));
    }

    @GetMapping("/orders")
    public List<OrderDto> getOrders( @AuthenticationPrincipal Jwt principal) {
        return orderService.getAllOrders(principal.getSubject());
    }

    @GetMapping("/orders/status/{status}")
    public List<OrderDto> getOrdersByStatus(@AuthenticationPrincipal Jwt principal, @PathVariable String status) {
        return orderService.getUserOrdersByStatus(principal.getSubject(), status);
    }

    @PutMapping("/orders/{id}")
    public OrderDto updateOrder(@PathVariable String id, @RequestBody OrderRequest orderRequest, @AuthenticationPrincipal Jwt principal) {
        return orderService.updateOrder(id, principal.getSubject(), OrderDto.fromRequest(orderRequest));
    }

    @DeleteMapping("/orders/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable String id,  @AuthenticationPrincipal Jwt principal) {
        orderService.deleteOrder(id, principal.getSubject());
    }




}
