package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Qualifier("createTopic")
    @Autowired
    private ChannelTopic topic;

    @Qualifier("updateTopic")
    @Autowired
    private ChannelTopic updateTopic;

    @Autowired
    private WalletRepository walletRepository;


    @Override
    public OrderDto createOrder(String userId, OrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(userId);
        order.setSide(Side.valueOf(orderRequest.getSide().toUpperCase()));
        order.setQuantity(orderRequest.getQuantity());
        order.setTicker(orderRequest.getTicker());
        order.setPrice(orderRequest.getPrice());
        order.setStatus(OrderStatus.PENDING);

        OrderDto orderDto = OrderDto.fromModel(orderRepository.save(order));

//        stringRedisTemplate.convertAndSend(topic.getTopic(), orderDto.getId());

        return orderDto;
    }

    @Override
    public OrderDto updateOrder(String id, String userId, OrderRequest orderRequest) {
        Order order = orderRepository.findByIdAndUserId(id, userId).orElseThrow(() ->
                new OrderNotFoundException(String.format("order with id %s does not exists", id)));

        order.setPrice(orderRequest.getPrice());
        order.setQuantity(orderRequest.getQuantity());

        order = orderRepository.save(order);

//        stringRedisTemplate.convertAndSend(updateTopic.getTopic(), order.getId());

        return OrderDto.fromModel(order);
    }

    @Override
    public List<OrderDto> findOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream().map(OrderDto::fromModel).collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getUserOrdersByStatus(String userId, String status) {
        return orderRepository.findByUserIdAndStatus(userId, status.toUpperCase()).stream().map(OrderDto::fromModel).collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(String id, String userId) {
        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("order with id %s does not exists", id)));

        orderRepository.delete(order);
    }

    @Override
    public List<OrderDto> getAllOrders(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderDto::fromModel)
                .collect(Collectors.toList());
    }

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
            Wallet wallet = walletRepository.findByUserId(actualOrder.getUserId()).orElse(new Wallet());


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
