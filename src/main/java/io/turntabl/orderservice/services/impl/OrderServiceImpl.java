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
import io.turntabl.orderservice.services.RedisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.annotation.WebListener;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * This is the order service default implementation
 * It defines the definition of all behaviours in relation to Order Creation
 */
@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private  OrderRepository orderRepository;
    @Autowired
    private  WalletRepository walletRepository;
    @Autowired
    private  RedisService redisService;
    @Autowired
    private  WebClient webClient;

    @Value("${matraining.exchange.one.token}")
    private String exchangeOneApiKey;

    @Value("${matraining.exchange.two.token}")
    private String exchangeTwoApiKey;


    /**
     * This method Creates an order based on the client's subject ID
     * @param userId refers to the Subject ID from the OAUTH server
     * @param requestDTO refers to the order request placed by the client
     * @return an Order DTO
     */
    @Override
    public OrderDto createOrder(String userId, OrderDto requestDTO) {

        Order order = Order.fromDto(requestDTO);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);

        OrderDto responseDTO = OrderDto.fromEntity(orderRepository.save(order));

        // Ensuring Order is persisted before publishing to topic for validation
        redisService.convertAndSendToCreateOrderTopic(responseDTO.getId());

        return responseDTO;
    }


    /**
     * @param existingOrderId refers tp tje existing order Id
     * @param userId          the user id
     * @param requestDTO refers to the incoming request dto
     * @return orderDTO
     */
    @Override
    public OrderDto updateOrder(String existingOrderId, String userId, OrderDto requestDTO) {

        Order order = orderRepository.findByIdAndUserId(existingOrderId, userId).orElseThrow(() ->
                new OrderNotFoundException(String.format("order with id %s does not exists", existingOrderId)));

        order.setPrice(requestDTO.getPrice());
        order.setQuantity(requestDTO.getQuantity());

        order = orderRepository.save(order);

        //todo: Send order updated event
        redisService.convertAndSendToUpdateOrderTopic(order.getId());

        return OrderDto.fromEntity(order);
    }

    /**
     * @param status the status
     * @return
     */
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
    public void cancelOrder(String orderId, String legId, String userId) {

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("order with id %s does not exists", orderId)));

        // Make a call to cancel order on exchange. If successful set status to canceled
        Optional<Order> updatedOrder = order.getOrderInformation()
                .stream()
                .filter(orderInfo -> orderInfo.getOrderId().equals(legId))
                .findFirst()
                .map(orderInfo -> {
                    String exchangeId = orderInfo.getExchangeUrl().equalsIgnoreCase("97b5fa07-08a2-43e8-9df8-9071a48da02c") ? exchangeOneApiKey : exchangeTwoApiKey;
                    boolean result = Boolean.TRUE.equals(webClient
                            .delete()
                            .uri(orderInfo.getExchangeUrl() + "/" + exchangeId + "/order/" + orderInfo.getOrderId())
                            .retrieve()
                            .bodyToMono(Boolean.class)
                            .doOnError(throwable -> log.info("Error Occurred during order cancellation {}", orderInfo.getOrderId()))
                            .onErrorReturn(false).block());
                    orderInfo.setStatus(result ? OrderItemStatus.CANCEL : OrderItemStatus.FULFILLED);
                    return orderRepository.save(order);
                });
        log.info("{}" , updatedOrder.orElse(new Order()));
    }

    @Override
    public List<OrderDto> getAllOrders(String userId, String status) {
        if (status == null || status.equals("")) {
            return orderRepository.findByUserId(userId)
                    .stream()
                    .map(OrderDto::fromEntity)
                    .collect(Collectors.toList());
        }
        return orderRepository.findByUserIdAndStatus(userId, status.toUpperCase())
                .stream()
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }


    @Override
    public List<OrderDto> findTotalOrders(){
        return orderRepository.findAll()
                .stream().map(OrderDto::fromEntity)
                .collect(Collectors.toList());
    }

    // TODO: Refactor this block
    @Override
    public void updateOrderStatus(String orderId, String orderItemId, OrderItemStatus status, int quantityFulfilled) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("order with id %s does not exists", orderId)));

        /*
            find order item
         */
        OrderInformationDto orderItem = order.getOrderInformation()
                .stream().filter(item -> item.getOrderId().equals(orderItemId))
                .findFirst()
                .orElse(new OrderInformationDto());

        log.info("Updating Order Item {}", orderItem);


        orderItem.setOrderId(orderItemId);
        orderItem.setStatus(status);
        orderItem.setQuantityFulfilled(quantityFulfilled == -1 ? orderItem.getQuantity() : quantityFulfilled);

        /*
            find the other order items excluding the one we are updating
         */
        List<OrderInformationDto> items = order.getOrderInformation()
                .stream()
                .filter(orderInfo -> !orderInfo.getOrderId().equals(orderItem.getOrderId()))
                .collect(Collectors.toList());

        // add the updated order item to the list
        items.add(orderItem);

        // Update Order
        order.setOrderInformation(items);


        long fulfilledItems = order
                .getOrderInformation()
                .stream().filter(x -> x.getStatus() == OrderItemStatus.FULFILLED)
                .mapToInt(OrderInformationDto::getQuantityFulfilled)
                .sum();

        if (fulfilledItems == order.getQuantity()) {

            order.setStatus(OrderStatus.CLOSED);

            /*
                update portfolio
             */
            Wallet wallet = walletRepository.findById(order.getUserId()).orElse(new Wallet());


            PortfolioDto portfolio = wallet
                    .getPortfolios()
                    .stream()
                    .filter(x -> x.getTicker().equals(order.getTicker()))
                    .findFirst()
                    .orElse(new PortfolioDto());

            portfolio.setTicker(order.getTicker());

            int quantity = order.getSide() == Side.SELL ? portfolio.getQuantity() - orderItem.getQuantity()
                    : portfolio.getQuantity() + orderItem.getQuantity();

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

        orderRepository.save(order);


    }

}
