package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.constants.ExchangeName;
import io.turntabl.orderservice.constants.OrderItemStatus;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.*;
import io.turntabl.orderservice.exceptions.WalletNotFoundException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.OrderBook;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.OrderBookRepository;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.requests.MalonOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    @Autowired
    private OrderBookRepository orderBook;

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


        System.out.println(receivedOrder.toString());

        switch (receivedOrder.getSide()){
            case BUY: buyOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne);
            break;
            case SELL: sellOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne);
            break;
        }

    }

    private void buyOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeDto) {
        /**
         * 1. Verify User has sufficient funds
         * 2. Buy-executions should be in order by the lowest price in the market
         * 3. Executions to exchange should be bound to single order
         * 4.
         *
         *
         * NB: Keep track of execution price and order
         */

        log.info("THIS IS MY CURRENT POSITION");

        Wallet wallet = walletRepository.findById(receivedOrder.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet information for " + receivedOrder.getUserId() + " not found"));

        // Check for sufficient balance
        if (wallet.getBalance() >= receivedOrder.getQuantity() * receivedOrder.getPrice() ){
            log.info("Balance Condition {}", wallet.getBalance() );

            log.info("{}",exchangeOneData);

            log.info("{}",exchangeTwoData);

            if (receivedOrder.getPrice() <=
                    exchangeOneData.getLastTradedPrice() + exchangeOneData.getMaxPriceShift()
                    ||
                receivedOrder.getPrice() <=
                        exchangeTwoData.getLastTradedPrice() + exchangeOneData.getMaxPriceShift()

            ){
                log.info("REACHED ===========");
                // Reach into Search for sum of quantities available that matches the quantity to be traded
                System.out.println(Side.SELL.name());
              orderBook.findFirst100ByProductAndSideOrderByPriceAscLocalDateTimeDesc(receivedOrder.getTicker(),Side.SELL.toString())
                      .forEach(System.out::println);

//                System.out.println(itemBySideAndPrice);

//                int quantity = 0;
//                List<OrderBook> orderBooks = new ArrayList<>();
//
//                for (OrderBook book : itemBySideAndPrice) {
//
//                    if (quantity >= receivedOrder.getQuantity()) {
//                        break;
//                    }else {
//                        log.info("order book = {}", book);
//                        log.info("Summed Quantity = {}", quantity);
//                        orderBooks.add(book);
//                        quantity += book.getQuantity();
//                    }
//
//                }

//                log.info("ALL COLLECTED ITEMS = {}", received.collect(Collectors.toList()));

            }



        }

    }

    private void sellOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeDto) {

        /**
         * 1. Verify User owns the quantity of stock to be sold
         * 2. Sell should not be less than average buy price
         *
         */

        double totalSellLimit = exchangeOneData.getSellLimit() + exchangeTwoData.getSellLimit();

        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();

        // if side is sell, check if user owns more or equal to the quantity of products being sold
        Wallet wallet = walletRepository.findById(receivedOrder.getUserId()).orElse(new Wallet());

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

//        sendOrderToExchange(receivedOrder,exchangeDto, receivedOrder.getQuantity());

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
