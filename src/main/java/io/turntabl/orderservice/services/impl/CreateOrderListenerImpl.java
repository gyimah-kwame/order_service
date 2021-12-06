package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.constants.ExchangeName;
import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.*;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.requests.MalonOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CreateOrderListenerImpl implements MessageListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Gson gson;

    @Autowired
    private HashOperations<String, String, String> hashOperations;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WebClient webClient;

    @Value("${matraining.token}")
    private String apiKey;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("create topic {}", message.toString());

        Optional<Order> order = orderRepository.findById(message.toString());

        if (order.isEmpty()) return;

        // validation goes here
        Order receivedOrder = order.get();

        String exchangeOneStr = hashOperations.get(ExchangeName.EXCHANGE_ONE.toString(),ExchangeName.EXCHANGE_ONE.toString());
        String exchangeTwoStr = hashOperations.get(ExchangeName.EXCHANGE_TWO.toString(),ExchangeName.EXCHANGE_TWO.toString());

        //convert exchange str to objects
        ExchangeDto exchangeOne = gson.fromJson(exchangeOneStr, ExchangeDto.class);
        ExchangeDto exchangeTwo = gson.fromJson(exchangeTwoStr, ExchangeDto.class);

        log.info("aa {}", exchangeOne);

        // keys to retrieve best prices from redis
        String exchangeOneKey = receivedOrder.getTicker()+"_"+exchangeOne.getId();
        String exchangeTwoKey = receivedOrder.getTicker()+"_"+exchangeTwo.getId();


        //get objects with best prices
        MarketDataDto exchangeOneData = gson.fromJson(hashOperations.get(exchangeOneKey, exchangeOneKey), MarketDataDto.class);
        MarketDataDto exchangeTwoData = gson.fromJson(hashOperations.get(exchangeTwoKey, exchangeTwoKey), MarketDataDto.class);


        if (receivedOrder.getSide() == Side.BUY) {
            buyOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne);
        }else {
            sellOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne);
        }

    }

    private void buyOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeDto) {

//        double totalBuyLimit = exchangeOneData.getBuyLimit() + exchangeTwoData.getBuyLimit();
//
//        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();
//
//        // if side is sell, check if user owns more or equal to the quantity of products being sold
//        Wallet wallet = walletRepository.findByUserId(receivedOrder.getUserId()).orElse(new Wallet());
//
//
//        if (wallet.getBalance() < price) {
//            return;
//        }
//
//        //perform limit checking on buy
//        if (totalBuyLimit != 0 && receivedOrder.getQuantity() > totalBuyLimit) {
//            return;
//        }

        double buyPrice = Math.min(exchangeOneData.getAskPrice(), exchangeTwoData.getAskPrice());;


        sendOrderToExchange(receivedOrder,exchangeDto, receivedOrder.getQuantity());

    }

    private void sellOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeDto) {
        double totalSellLimit = exchangeOneData.getSellLimit() + exchangeTwoData.getSellLimit();

        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();

        // if side is sell, check if user owns more or equal to the quantity of products being sold
        Wallet wallet = walletRepository.findByUserId(receivedOrder.getUserId()).orElse(new Wallet());

        //perform limit checking on sell
//        if (receivedOrder.getQuantity() > totalSellLimit) {
//            return;
//        }


        Optional<PortfolioDto> portfolio = wallet.getPortfolios()
                .stream()
                .filter(x -> x.getTicker().equalsIgnoreCase(receivedOrder.getTicker()) && x.getQuantity() >= receivedOrder.getQuantity())
                .findFirst();

//        if (portfolio.isEmpty()) {
//            return;
//        }

        sendOrderToExchange(receivedOrder,exchangeDto, receivedOrder.getQuantity());

    }

    private void sendOrderToExchange(Order order, ExchangeDto exchangeDto, int quantity) {

        if (order.getStatus() == OrderStatus.PROCESSING) return;

        log.info("sending to exchange {}", exchangeDto.getBaseUrl());

        MalonOrderRequest malonOrderRequest = MalonOrderRequest.fromOrder(order);

        ResponseEntity<String> response = webClient.post()
                .uri(exchangeDto.getBaseUrl()+"/"+apiKey+"/order")
                .body(Mono.just(malonOrderRequest), MalonOrderRequest.class)
                .retrieve()
                .toEntity(String.class)
                .block();

        if (response == null) return;

        Optional<String> body = Optional.ofNullable(response.getBody());


        if (response.getStatusCode().is2xxSuccessful()) {

            String orderId = body.orElse("").substring(1, response.getBody().length()-1);

            order.getOrderInformation().add(new OrderInformationDto(exchangeDto.getBaseUrl(), orderId, quantity, 0, OrderItemStatus.PENDING));

            order.setStatus(OrderStatus.PROCESSING);

            orderRepository.save(order);
        }
    }
}
