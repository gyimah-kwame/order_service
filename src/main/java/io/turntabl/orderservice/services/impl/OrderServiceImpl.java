package io.turntabl.orderservice.services.impl;

import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.dtos.OrderInformationDto;
import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.exceptions.OrderNotFoundException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.requests.OrderRequest;
import io.turntabl.orderservice.services.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * This is the order service default implementation
 * It defines the definition of all behaviours in relation to Order Creation
 */
@Service
@Transactional
@Slf4j
@AllArgsConstructor // This allows for auto wiring through constructor for dependencies.
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic createOrderTopic;
    private final WalletRepository walletRepository;


    /**
     * This method Creates an order based on the client's subject ID
     * @param userId refers to the Subject ID from the OAUTH server
     * @param requestDTO refers to the order request placed by the client
     * @return an Order DTO
     */
    @Override
    public OrderDto createOrder(String userId, OrderDto requestDTO) {

        Order order = Order.fromDto(requestDTO);
        order.setStatus(OrderStatus.PENDING);
        OrderDto responseDTO = OrderDto.fromEntity(orderRepository.save(order));
        stringRedisTemplate.convertAndSend(createOrderTopic.getTopic(), responseDTO.getId());
        return responseDTO;
    }


    @Override
    public OrderDto updateOrder(String existingOrderId, String userId, OrderDto requestDTO) {

        Order order = orderRepository.findByIdAndUserId(existingOrderId, userId).orElseThrow(() ->
                new OrderNotFoundException(String.format("order with id %s does not exists", existingOrderId)));

        order.setPrice(requestDTO.getPrice());
        order.setQuantity(requestDTO.getQuantity());

        order = orderRepository.save(order);
//        stringRedisTemplate.convertAndSend(updateTopic.getTopic(), order.getId());

        return OrderDto.fromEntity(order);
    }

    @Override
    public List<OrderDto> findOrdersByStatus(String status) {
        return orderRepository.findByStatus(status)
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getUserOrdersByStatus(String userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status.toUpperCase())
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(String orderID, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderID, userId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("order with id %s does not exists", orderID)));

        orderRepository.delete(order);
    }

    @Override
    public List<OrderDto> getAllOrders(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }



    // TODO: Refactor this block
    @Override
    public void updateOrderStatus(String orderId, String orderItemId, OrderItemStatus status, int quantityFulfilled) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) return;

        Order actualOrder = order.get();

        /*
            find order item id
         */
        OrderInformationDto orderItem = actualOrder.getOrderInformation()
                .stream().filter(item -> item.getOrderId().equals(orderItemId))
                .findFirst()
                .orElse(new OrderInformationDto());

        log.info("item {}", orderItem);

        orderItem.setOrderId(orderId);
        orderItem.setStatus(status);
        orderItem.setQuantityFulfilled(quantityFulfilled == -1 ? orderItem.getQuantity() : quantityFulfilled);

        /*
            find the other order items excluding the one we are updating
         */
        List<OrderInformationDto> items = actualOrder.getOrderInformation()
                .stream()
                .filter(x -> !x.getOrderId().equals(orderItem.getOrderId()))
                .collect(Collectors.toList());

        // add the updated order item to the list
        items.add(orderItem);

        actualOrder.setOrderInformation(items);


        long fulfilledItems = actualOrder
                .getOrderInformation()
                .stream().filter(x -> x.getStatus() == OrderItemStatus.FULFILLED)
                .count();

        if (fulfilledItems == actualOrder.getOrderInformation().size()) {

            actualOrder.setStatus(OrderStatus.FULFILLED);

            /*
                update portfolio
             */
            Wallet wallet = walletRepository.findById(actualOrder.getUserId()).orElse(new Wallet());


            PortfolioDto portfolio = wallet
                    .getPortfolios()
                    .stream()
                    .filter(x -> x.getTicker().equals(actualOrder.getTicker()))
                    .findFirst()
                    .orElse(new PortfolioDto());

            portfolio.setTicker(actualOrder.getTicker());

            int quantity = actualOrder.getSide() == Side.SELL ? portfolio.getQuantity() - actualOrder.getQuantity()
                    : portfolio.getQuantity() + actualOrder.getQuantity();

            portfolio.setQuantity(quantity);

            List<PortfolioDto> updatePortfolios = wallet
                    .getPortfolios()
                    .stream()
                    .filter(p -> !p.getTicker().equals(portfolio.getTicker()))
                    .collect(Collectors.toList());

            updatePortfolios.add(portfolio);

            wallet.setPortfolios(updatePortfolios);

            walletRepository.save(wallet);


        }

        orderRepository.save(actualOrder);


    }

}
