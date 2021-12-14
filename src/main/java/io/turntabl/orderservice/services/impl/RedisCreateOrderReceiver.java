package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.dtos.ExchangeDto;
import io.turntabl.orderservice.dtos.MarketDataDto;
import io.turntabl.orderservice.dtos.OrderInformationDto;
import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.enums.ExchangeName;
import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.exceptions.InsufficientWalletBalanceException;
import io.turntabl.orderservice.exceptions.InvalidOrderException;
import io.turntabl.orderservice.exceptions.OrderNotFoundException;
import io.turntabl.orderservice.exceptions.WalletNotFoundException;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.models.tickers.Ticker;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import io.turntabl.orderservice.repositories.tickers.*;
import io.turntabl.orderservice.requests.MalonOrderRequest;
import io.turntabl.orderservice.services.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RedisCreateOrderReceiver {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private RedisService redisService;
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

    @Value("${matraining.exchange.one.token}")
    private String exchangeOneApiKey;

    @Value("${matraining.exchange.two.token}")
    private String exchangeTwoApiKey;


    @Autowired
    private Gson gson;


    /**
     * Processes Incoming Message Converted
     * @param message
     */
    public void createOrderMessageConsumer(String message){

        log.info("New Order to be processed {}", message);
        /*
         *  1. Verify Order Exists in Database and Delegate based on SIDE
         */
        Order order = orderRepository.findById(message).
                orElseThrow(() -> new OrderNotFoundException("Order with ID " + message + " tobe validated and processed does not exist."));

        try {

            Order finalOrder = order;
            Wallet wallet = walletRepository.findById(order.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet information for " + finalOrder.getUserId() + " not found", finalOrder.getId()));
            log.info("Wallet for user successfully obtained : {}", wallet);

            ExchangeDto exchangeOneDetails = getExchangeDetailsFromHash(ExchangeName.EXCHANGE_ONE);
            ExchangeDto exchangeTwoDetails = getExchangeDetailsFromHash(ExchangeName.EXCHANGE_TWO);

            if (exchangeOneDetails.isActive() || exchangeTwoDetails.isActive()) {
                switch (order.getSide()) {
                    case BUY:
                        log.info("Buy Operation being placed, for order {}", order);
                        order = buyOperation(order, wallet, exchangeOneDetails, exchangeTwoDetails);
                        break;
                    case SELL:
                        log.info("Sell Operation being placed, for order {}", order);
                        order = sellOperation(order, wallet, exchangeOneDetails, exchangeTwoDetails);
                        break;
                }


            } else {
                // Update order Status to PENDING with Message No Active Exchanges so a background process will process this if exchanges become available
                order.setStatus(OrderStatus.PENDING);
                order.setStatusInfo("No exchange is active at the moment.");
                orderRepository.save(order);
                log.info("No Exchange is active at the moment.");
            }
        }catch (WalletNotFoundException exception){
            log.info("Wallet information not found for order {}", order);
            order.setStatus(OrderStatus.INVALID);
            order.setStatusInfo(exception.getMessage() + "ORDER ID :" + exception.getOrderId());
            order = orderRepository.save(order);
        }

        log.info("State of Order order: {}", order);

    }

    @Transactional
    Order  buyOperation(Order receivedOrder, Wallet wallet, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {
        /* 0. TODO: Verify Daily Buy Limit is not up. (We have a buy limit for each product)
         * 1. Verify User has sufficient funds
         * 2. Buy-executions should be in order by the lowest price in the market
         * 3. Executions to exchange should be bound to single order
         * NB: Keep track of execution price and order
         */
        try{
            // Sufficient Bal Check
            if (wallet.getBalance() >= receivedOrder.getQuantity() * receivedOrder.getPrice() ){
                // Reach into Search for sum of quantities available that matches the quantity to be traded
                MarketDataDto marketDataDtoOne = redisService.getMarketDataFromHash(receivedOrder.getTicker(), exchangeOne.getId());
                MarketDataDto marketDataDtoTwo = redisService.getMarketDataFromHash(receivedOrder.getTicker(), exchangeTwo.getId());

                // received order's price is not less than the market bid price
                if (
                        (receivedOrder.getPrice() < marketDataDtoOne.getBidPrice()) && (receivedOrder.getPrice() < marketDataDtoTwo.getBidPrice())
                ) {
                    receivedOrder.setStatus(OrderStatus.INVALID);
                    receivedOrder.setStatusInfo("order price too low for market");

                    return orderRepository.save(receivedOrder);
                }

                List<? extends Ticker> availableFinancialProductsOnMarket = findAppropriateTickerInformationFromOrderForBuyOperation(receivedOrder, exchangeOne,exchangeTwo);

                if (availableFinancialProductsOnMarket.isEmpty()){
                    receivedOrder = processFinancialProducts(receivedOrder, exchangeOne, exchangeTwo, marketDataDtoOne, marketDataDtoTwo, receivedOrder.getQuantity());
                }else {

                // Publish information to exchange
                int quantitySent = 0;
                for (Ticker marketProductForSale : availableFinancialProductsOnMarket) {
                    // 1. Get to know the exchange
                    ExchangeDto exchange = exchangeOne.getBaseUrl().equalsIgnoreCase(marketProductForSale.getExchangeURL()) ? exchangeOne : exchangeTwo;

                    if (quantitySent < receivedOrder.getQuantity() )   {

                        receivedOrder.setStatus(OrderStatus.OPEN);
                        receivedOrder.setStatusInfo("Order partially Processed");
                        receivedOrder = orderRepository.save(receivedOrder);

                        int quantityToSend = receivedOrder.getQuantity() - quantitySent;

                        if(marketProductForSale.getQuantity() > quantityToSend ){
                            // Reduce Account Balance with Corresponding Amount
                            wallet = updateBalanceOfClientsWallet(wallet, (quantityToSend * marketProductForSale.getPrice()));
                            receivedOrder = updateOrderStatusForQuantityProcessed(receivedOrder,quantityToSend);

                            String orderIdReturnedFromExchange = sendOrderToExchange(
                                    receivedOrder,
                                    marketProductForSale.getPrice(),
                                    exchange,
                                    quantityToSend
                            );
                            receivedOrder.getOrderInformation()
                                    .add(new OrderInformationDto(exchange.getBaseUrl(), orderIdReturnedFromExchange, quantityToSend, 0, OrderItemStatus.PENDING, marketProductForSale.getPrice()));
                            receivedOrder = orderRepository.save(receivedOrder);
                            quantitySent += quantityToSend ;

                        }else {
                            wallet = updateBalanceOfClientsWallet(wallet, marketProductForSale.getQuantity() * marketProductForSale.getPrice());
                            receivedOrder = updateOrderStatusForQuantityProcessed(receivedOrder, marketProductForSale.getQuantity());

                            String orderIdReturnedFromExchange = sendOrderToExchange(
                                    receivedOrder,
                                    marketProductForSale.getPrice(),
                                    exchange,
                                    marketProductForSale.getQuantity()
                            );

                                receivedOrder.getOrderInformation()
                                        .add(new OrderInformationDto(
                                                exchange.getBaseUrl(),
                                                orderIdReturnedFromExchange,
                                                marketProductForSale.getQuantity(),
                                                0,
                                                orderIdReturnedFromExchange.equals("") ? OrderItemStatus.FAILED : OrderItemStatus.PENDING,
                                                marketProductForSale.getPrice()
                                            )
                                        );
                                receivedOrder = orderRepository.save(receivedOrder);
                                quantitySent += marketProductForSale.getQuantity();



                        }
                    }else {
                        if (receivedOrder.getQuantity() > quantitySent ){
                            // Perform Personal Calculations and Send to exchange
                            receivedOrder = processFinancialProducts(receivedOrder, exchangeOne, exchangeTwo, marketDataDtoOne, marketDataDtoTwo, receivedOrder.getQuantity() - quantitySent);
                            quantitySent += (receivedOrder.getQuantity() - quantitySent) ;
                        }
                        receivedOrder.setStatus(OrderStatus.OPEN);
                        receivedOrder.setStatusInfo("Order processed");
                        receivedOrder = orderRepository.save(receivedOrder);
                    }
                }

                }
            }
            else{

                throw new InsufficientWalletBalanceException("Wallet balance is insufficient for the order");
            }

        }catch (InsufficientWalletBalanceException exception){
            receivedOrder.setStatus(OrderStatus.INVALID);
            receivedOrder.setStatusInfo(exception.getMessage());
            receivedOrder = orderRepository.save(receivedOrder);
        }

        return  receivedOrder;
    }

    private Order processFinancialProducts(Order receivedOrder,
                                           ExchangeDto exchangeOne,
                                           ExchangeDto exchangeTwo,
                                           MarketDataDto marketDataDtoOne,
                                           MarketDataDto marketDataDtoTwo,
                                           int quantity) {
        MarketDataDto marketDataDto = marketDataDtoOne.getBidPrice() < marketDataDtoTwo.getBidPrice() ? marketDataDtoOne : marketDataDtoTwo;
        double price = marketDataDto.getBidPrice() - (0.09 *  marketDataDto.getBidPrice());
        ExchangeDto exchange = marketDataDto.getExchangeId().equals(exchangeOne.getId()) ? exchangeOne : exchangeTwo;
        String orderIdReturnedFromExchange = sendOrderToExchange(
                receivedOrder,
                price,
                exchange,
                quantity
        );

        receivedOrder.getOrderInformation()
                .add(new OrderInformationDto(exchange.getBaseUrl(), orderIdReturnedFromExchange, quantity, 0, OrderItemStatus.PENDING, price));
        receivedOrder = orderRepository.save(receivedOrder);
        return receivedOrder;
    }



    @Transactional
    Order sellOperation(Order receivedOrder,Wallet wallet, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {

        /* 1. Verify User owns the quantity of stock to be sold (Quantity to SELL )
         * 2. SELL-executions should be in order by the GREATEST price in the market
         * 3. Executions to exchange should be bound to single order
         * NB: Keep track of execution price and order
         */
        try{

            Order finalReceivedOrder = receivedOrder;
            PortfolioDto itemToSell = wallet.getPortfolios()
                    .stream()
                    .filter(
                            portfolioItem -> portfolioItem.getTicker()
                                    .equalsIgnoreCase(finalReceivedOrder.getTicker()) &&
                                    portfolioItem.getQuantity() >= finalReceivedOrder.getQuantity()
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new InvalidOrderException("Invalid Sell Operation. Insufficient Quantity of " + finalReceivedOrder.getTicker() + " in account")
                    );

            MarketDataDto marketDataDtoOne = redisService.getMarketDataFromHash(receivedOrder.getTicker(), exchangeOne.getId());
            MarketDataDto marketDataDtoTwo = redisService.getMarketDataFromHash(receivedOrder.getTicker(), exchangeTwo.getId());


            List<? extends Ticker> availableFinancialProductsOnMarket = findAppropriateTickerInformationFromOrderForSellOperation(receivedOrder, exchangeOne, exchangeTwo);
            /*
             * 1. Put tickers into 2 groups based on Highest Demand
             * 2. Check the exchange/exchanges involved
             * TODO:
             * 3. Put product to sell into 5 batches
             * 4. Execute each batch with a different algorithm
             *  ==== ALGOS
             *    a) Sell first Batch at Highest price on market
             *    b) Sell next batch at (Average of best 5 prices| A ) + (0.1 of A)321  q54R3EWQ    1
             *    c) Sell  next batch at (A + MAX_PRICE_SHIFT | B ) - 0.3 B | Verify if greater than allowed price
             *    d) Sell  next batch at (A + 0.17*A)
             *    e) Sell  next batch at (A + 0.2*A)
             */
            if (availableFinancialProductsOnMarket.isEmpty()){

                MarketDataDto marketDataDto = marketDataDtoOne.getAskPrice() > marketDataDtoTwo.getAskPrice() ? marketDataDtoOne : marketDataDtoTwo;
                ExchangeDto exchange = exchangeOne.getId().equals(marketDataDto.getExchangeId()) ? exchangeOne : exchangeTwo;
                double price = marketDataDto.getAskPrice() + (0.09 *  marketDataDto.getAskPrice());
                String internalID = sendOrderToExchange(receivedOrder, price, exchange, receivedOrder.getQuantity());

                receivedOrder
                        .getOrderInformation()
                        .add(new OrderInformationDto(exchange.getBaseUrl(),
                                internalID, receivedOrder.getQuantity(), 0, OrderItemStatus.PENDING, price));

            }else {
                for (Ticker marketProductForSale : availableFinancialProductsOnMarket) {

                    int quantitySent = 0;
                    ExchangeDto exchange = exchangeOne.getBaseUrl().equalsIgnoreCase(marketProductForSale.getExchangeURL()) ? exchangeOne : exchangeTwo;
                    receivedOrder.setStatus(OrderStatus.OPEN);
                    if (quantitySent < receivedOrder.getQuantity()) {

                        receivedOrder.setStatusInfo("Order partially Processed");
                        receivedOrder = orderRepository.save(receivedOrder);

                        int quantityToSend = receivedOrder.getQuantity() - quantitySent;
                        if (marketProductForSale.getQuantity() > quantityToSend) {
                            itemToSell.setQuantity(itemToSell.getQuantity() - quantityToSend);
                            wallet = walletRepository.save(wallet);
                            String idReturnedFromExchange = sendOrderToExchange(receivedOrder, marketProductForSale.getPrice(), exchange, quantityToSend);
                            receivedOrder
                                    .getOrderInformation()
                                    .add(new OrderInformationDto(exchange.getBaseUrl(),
                                            idReturnedFromExchange, quantityToSend, 0, OrderItemStatus.PENDING, marketProductForSale.getPrice()));

                        } else {
                            itemToSell.setQuantity(receivedOrder.getQuantity());
                            wallet = walletRepository.save(wallet);
                            String idReturnedFromExchange = sendOrderToExchange(receivedOrder, marketProductForSale.getPrice(), exchange, marketProductForSale.getQuantity());
                            receivedOrder
                                    .getOrderInformation()
                                    .add(new OrderInformationDto(exchange.getBaseUrl(),
                                            idReturnedFromExchange, quantityToSend, 0, OrderItemStatus.PENDING, marketProductForSale.getPrice()));
                        }
                    } else {

                        if(receivedOrder.getQuantity() > quantitySent){
                            MarketDataDto marketDataDto = marketDataDtoOne.getAskPrice() > marketDataDtoTwo.getAskPrice() ? marketDataDtoOne : marketDataDtoTwo;
                            ExchangeDto exchangeDto = exchangeOne.getId().equals(marketDataDto.getExchangeId()) ? exchangeOne : exchangeTwo;
                            double price = marketDataDto.getAskPrice() + (0.09 *  marketDataDto.getAskPrice());
                            String internalID = sendOrderToExchange(receivedOrder, price, exchangeDto, receivedOrder.getQuantity());

                            receivedOrder
                                    .getOrderInformation()
                                    .add(new OrderInformationDto(exchangeDto.getBaseUrl(),
                                            internalID, receivedOrder.getQuantity(), 0, OrderItemStatus.PENDING, price));
                        }
                        receivedOrder.setStatusInfo("Order processed");
                        receivedOrder = orderRepository.save(receivedOrder);
                    }

                }
            }
        }catch (InvalidOrderException exception){
            receivedOrder.setStatus(OrderStatus.INVALID);
            receivedOrder.setStatusInfo(exception.getMessage());
            receivedOrder = orderRepository.save(receivedOrder);
        }
        return receivedOrder;
    }

    private String sendOrderToExchange(Order order,double price, ExchangeDto exchangeDto, int quantity) {

        log.info("sending to exchange {}", exchangeDto);

        MalonOrderRequest malonOrderRequest = MalonOrderRequest.fromOrder(order,price,quantity);

        return webClient.post()
                .uri(exchangeDto.getBaseUrl()+"/"+ exchangeDto.getApiKey() +"/order")
                .body(Mono.just(malonOrderRequest), MalonOrderRequest.class)
                .retrieve()
                .bodyToMono(String.class)
                .map(received ->  received.substring(1, received.length()-1))
                .doOnError(throwable -> {
                    log.info("Processing Order Item failed");
                })
                .onErrorReturn("")
                .block();
    }

    private ExchangeDto getExchangeDetailsFromHash(ExchangeName exchangeName){
        ExchangeDto exchangeDto = redisService.getExchangeDetailsFromHash(exchangeName);
        exchangeDto.setApiKey(exchangeName.equals(ExchangeName.EXCHANGE_ONE)? exchangeOneApiKey : exchangeTwoApiKey);
        return exchangeDto;
    }

    private Wallet updateBalanceOfClientsWallet(Wallet wallet, double amountToDebitClientAccount) {
        wallet.setBalance(wallet.getBalance() - amountToDebitClientAccount);
        return walletRepository.save(wallet);
    }

    private Order updateOrderStatusForQuantityProcessed(Order receivedOrder, int quantityToProcess) {

        receivedOrder.setQuantityProcessed(receivedOrder.getQuantityProcessed() + quantityToProcess);
        return orderRepository.save(receivedOrder);
    }

    private List<? extends Ticker> findAppropriateTickerInformationFromOrderForBuyOperation
            (Order receivedOrder, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {
        List<? extends Ticker> items;
        switch (receivedOrder.getTicker()) {
            case "MSFT":
                items = microsoftRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "NFLX":
                items = netflixRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "GOOGL":
                items = googleRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "AAPL":
                items = appleRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "TSLA":
                items = teslaRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "IBM":
                items = ibmRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "ORCL":
                items = oracleRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            case "AMZN":
                items = amazonRepository.findTop25BySideOrderByPriceAsc(Side.SELL.name());
                break;
            default:
                items = new ArrayList<>();
                break;
        }

        if (items.isEmpty()){

            /* TODO: Provide Calculated Values for ticker
             * 1. Get The Latest Market Data from both exchanges
             * 2. Find Exchange with the lowest price for last sold | SELL ON ONLY VIABLE EXCHANGE
             * 3. Create tickers that are 10% and 15% less than the Market BID price
             * 4. Add them to the availableFinancialProductsOnMarket
             */
        }

        // Get exact number needed for processing;
        return getTickers(receivedOrder, exchangeOne, exchangeTwo, items);
    }

    private List<? extends Ticker> findAppropriateTickerInformationFromOrderForSellOperation
            (Order receivedOrder, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {
        List<? extends Ticker> items;
        switch (receivedOrder.getTicker()) {
            case "MSFT":
                items = microsoftRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "NFLX":
                items = netflixRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "GOOGL":
                items = googleRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "AAPL":
                items = appleRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "TSLA":
                items = teslaRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "IBM":
                items = ibmRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "ORCL":
                items = oracleRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            case "AMZN":
                items = amazonRepository.findTop5BySideOrderByPriceDesc(Side.BUY.name());
                break;
            default:
                items = new ArrayList<>();
                break;
        }
        if (items.isEmpty()){

            /* TODO: Provide Calculated Values for ticker
             * 1. Get The Latest Market Data from both exchanges
             * 2. Find Exchange with the lowest price for last sold | SELL ON ONLY VIABLE EXCHANGE
             * 3. Create tickers that are 10% and 15% less than the Market BID price
             * 4. Add them to the availableFinancialProductsOnMarket
             */
        }

        // Get exact number needed for processing;
        return getTickers(receivedOrder, exchangeOne, exchangeTwo, items);

 /*       int quantity = 0;
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

*/
    }

    private List<? extends Ticker> getTickers
            (Order receivedOrder, ExchangeDto exchangeOne, ExchangeDto exchangeTwo, List<? extends Ticker> items) {
        List<Ticker> selectedItems = new ArrayList<>();
        log.info("exchange one {}", exchangeOne);
        log.info("exchange two {}", exchangeTwo);
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
                quantity += item.getQuantity();
                selectedItems.add(item);
            }

            if(quantity >= receivedOrder.getQuantity()) break;

        }

        return selectedItems;

 /*       AtomicInteger totalToBuy = new AtomicInteger();

        return items.parallelStream()
                .filter(item -> (
                        exchangeOne.isActive() &&
                                exchangeOne.getBaseUrl().equalsIgnoreCase(item.getExchangeURL()) ||
                                exchangeTwo.isActive() &&
                                        exchangeTwo.getBaseUrl().equalsIgnoreCase(item.getExchangeURL())
                ))
                .takeWhile(ticker -> {
                    if (totalToBuy.get() <= receivedOrder.getQuantity()) {
                        totalToBuy.addAndGet(ticker.getQuantity());
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

  */
    }


}

