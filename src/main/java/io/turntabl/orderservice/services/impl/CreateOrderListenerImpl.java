package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.enums.ExchangeName;
import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.dtos.*;
import io.turntabl.orderservice.exceptions.WalletNotFoundException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.models.tickers.Ticker;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.repositories.tickers.*;
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

    @Autowired
    private MicrosoftRepository microsoftRepository;
    @Autowired
    private NetflixRepository netflixRepository;
    @Autowired
    private GoogleRepository googleRepository;
    @Autowired
    private AppleRepository appleRepository;
    @Autowired
    private TeslaRepository teslaRepository;
    @Autowired
    private IBMRepository ibmRepository;
    @Autowired
    private OracleRepository oracleRepository;
    @Autowired
    private AmazonRepository amazonRepository;

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

        // keys to retrieve best prices from redis
        String exchangeOneKey = receivedOrder.getTicker()+"_"+exchangeOne.getId();
        String exchangeTwoKey = receivedOrder.getTicker()+"_"+exchangeTwo.getId();

        //get objects with best prices
        MarketDataDto exchangeOneData = gson.fromJson(hashOperations.get(exchangeOneKey, exchangeOneKey), MarketDataDto.class);
        MarketDataDto exchangeTwoData = gson.fromJson(hashOperations.get(exchangeTwoKey, exchangeTwoKey), MarketDataDto.class);

        if (exchangeOne.isActive() || exchangeTwo.isActive()) {
            switch (receivedOrder.getSide()) {
                case BUY:
                    buyOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne, exchangeTwo);
                    break;
                case SELL:
                    sellOperation(receivedOrder, exchangeOneData, exchangeTwoData, exchangeOne, exchangeTwo);
                    break;
            }
        }

    }

    private void buyOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeOne,ExchangeDto exchangeTwo) {
        /**
         * 1. Verify User has sufficient funds
         * 2. Buy-executions should be in order by the lowest price in the market
         * 3. Executions to exchange should be bound to single order
         * NB: Keep track of execution price and order
         */
        try{
            Wallet wallet = walletRepository.findById(receivedOrder.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet information for " + receivedOrder.getUserId() + " not found", receivedOrder.getId()));
            if (wallet.getBalance() >= receivedOrder.getQuantity() * receivedOrder.getPrice() ){
                if (receivedOrder.getPrice() <=
                        exchangeOneData.getLastTradedPrice() + exchangeOneData.getMaxPriceShift()
                        ||
                        receivedOrder.getPrice() <=
                                exchangeTwoData.getLastTradedPrice() + exchangeOneData.getMaxPriceShift()

                ){
                    // Reach into Search for sum of quantities available that matches the quantity to be traded
                    List<? extends Ticker> tickers = findAppropriateTickerInformationFromOrder(receivedOrder, exchangeOne,exchangeTwo);
                    // Publish information to exchange

                    int quantitySent = 0;
                    for (Ticker ticker : tickers) {
                        if (quantitySent < receivedOrder.getQuantity() )   {
                            log.info("{}", ticker);
                            log.info("{}", quantitySent);
                            log.info("{}", receivedOrder);

                            if(ticker.getQuantity() > (receivedOrder.getQuantity() - quantitySent)){
                                log.info("{}",(receivedOrder.getQuantity() - quantitySent));
                                sendOrderToExchange(
                                        receivedOrder,
                                        exchangeOne
                                                .getBaseUrl()
                                                .equalsIgnoreCase(ticker.getExchangeURL())
                                                ? exchangeOne : exchangeTwo,
                                        (receivedOrder.getQuantity() - quantitySent)
                                );
                                quantitySent += (receivedOrder.getQuantity() - quantitySent) ;
                            }else {
                                log.info("{}",ticker.getQuantity());
                                sendOrderToExchange(
                                        receivedOrder,
                                        exchangeOne
                                                .getBaseUrl()
                                                .equalsIgnoreCase(ticker.getExchangeURL())
                                                ? exchangeOne : exchangeTwo,
                                        ticker.getQuantity()
                                );
                                quantitySent += ticker.getQuantity();
                            }
                        }
                    }


                }

            }

        }catch (WalletNotFoundException e){
            Order order = orderRepository.findById(e.getOrderId()).orElse(new Order());
            order.setStatus(OrderStatus.INVALID);
            orderRepository.save(order);
        }

    }

    private List<? extends Ticker> findAppropriateTickerInformationFromOrder(Order receivedOrder, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {
        List<? extends Ticker> items;
        switch (receivedOrder.getTicker()){
            case "MSFT": items = microsoftRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "NFLX": items = netflixRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "GOOGL": items = googleRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "AAPL": items = appleRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "TSLA": items = teslaRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "IBM": items = ibmRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "ORCL": items = oracleRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "AMZN": items = amazonRepository.findBySideOrderByPriceAsc(Side.SELL.name());
                break;
            default: items = new ArrayList<>();
                break;

        }
        // Get exact number needed for processing;
        List<Ticker> selectedItems = new ArrayList<>();
        int quantity = 0;
        for (Ticker item: items){
            if ((
                    exchangeOne.isActive() &&
                            exchangeOne.getBaseUrl().equalsIgnoreCase(item.getExchangeURL())
                            ||
                            exchangeTwo.isActive() &&
                            exchangeTwo.getBaseUrl().equalsIgnoreCase(item.getExchangeURL())
                    )
                    && receivedOrder.getQuantity() >= quantity
            ){
                quantity += receivedOrder.getQuantity();
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    private void sellOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData, ExchangeDto exchangeDto, ExchangeDto exchangeTwo) {

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
