package io.turntabl.orderservice;

import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.dtos.OrderInformationDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class OrderTest {

    @Mock
    private OrderRepository exchangeRepository;

    @Mock
    private OrderService orderService;

    List<OrderInformationDto> orderInformationDto = List.of(new OrderInformationDto("", "",
            3, 1, OrderItemStatus.PENDING));

    private final OrderDto orderDto = new OrderDto("1", orderInformationDto, "1",
            Side.SELL, 100.20,28, "TSLA", OrderStatus.PENDING, LocalDateTime.now());

    private final OrderDto orderDto2 = new OrderDto("2", orderInformationDto, "1",
            Side.BUY, 100.00,38, "MSFT", OrderStatus.PENDING, LocalDateTime.now()
    );

    private final OrderRequest orderRequest = new OrderRequest(Side.BUY.toString(), orderDto.getPrice(), orderDto.getQuantity(), orderDto.getTicker());

    @Test
    public void testGetAllOrders(){
        Mockito.when(orderService.getAllOrders("1")).thenReturn(new ArrayList<>(List.of(orderDto, orderDto2)));
        Assertions.assertEquals(2, orderService.getAllOrders("1").size());
    }

    @Test
    public void testStoreOrder() {
        Mockito.when(orderService.createOrder("1", orderRequest)).thenReturn(orderDto);

        OrderDto savedOrder = orderService.createOrder("1", orderRequest);

        Assertions.assertEquals(savedOrder.getTicker(), orderRequest.getTicker());
        Assertions.assertEquals(savedOrder.getQuantity(), orderRequest.getQuantity());
    }

    @Test
    public void testFindOrderByStatus() {
        Mockito.when(orderService.findOrdersByStatus("pending")).thenReturn(new ArrayList<>(List.of(orderDto, orderDto2)));

        Assertions.assertEquals(2, orderService.findOrdersByStatus("pending").size());

    }

    @Test
    public void testGetUserOrdersByStatus() {
        Mockito.when(orderService.getUserOrdersByStatus("1", "pending")).thenReturn(new ArrayList<>(List.of(orderDto)));
        Assertions.assertEquals(1, orderService.getUserOrdersByStatus("1", "pending").size());
    }


}
