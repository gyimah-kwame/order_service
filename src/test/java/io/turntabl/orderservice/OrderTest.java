package io.turntabl.orderservice;

import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.dtos.OrderInformationDto;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
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
    @DisplayName("Get All Orders")
    public void testGetAllOrders(){
        Mockito.when(orderService.getAllOrders("1")).thenReturn(List.of(orderDto, orderDto2));
        Assertions.assertEquals(2, orderService.getAllOrders("1").size());
    }

    @Test
    @DisplayName("Store new Order")
    public void testStoreOrder() {
        Mockito.when(orderService.createOrder("1", OrderDto.fromRequest(orderRequest))).thenReturn(orderDto);
        OrderDto savedOrder = orderService.createOrder("1", OrderDto.fromRequest(orderRequest));

        Assertions.assertEquals(savedOrder.getTicker(), orderRequest.getTicker());
        Assertions.assertEquals(savedOrder.getQuantity(), orderRequest.getQuantity());
    }

    @Test
    @DisplayName("Find Order By Status")
    public void testFindOrderByStatus() {
        Mockito.when(orderService.findOrdersByStatus("pending")).thenReturn(List.of(orderDto, orderDto2));

        Assertions.assertEquals(2, orderService.findOrdersByStatus("pending").size());

    }

    @Test
    @DisplayName("Get User Order By Status")
    public void testGetUserOrdersByStatus() {
        Mockito.when(orderService.getUserOrdersByStatus("1", "pending")).thenReturn(List.of(orderDto));
        Assertions.assertEquals(1, orderService.getUserOrdersByStatus("1", "pending").size());
    }


}
