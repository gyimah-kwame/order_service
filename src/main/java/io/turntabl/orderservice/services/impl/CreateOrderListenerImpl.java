package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.enums.ExchangeName;
import io.turntabl.orderservice.enums.OrderItemStatus;
import io.turntabl.orderservice.enums.OrderStatus;
import io.turntabl.orderservice.enums.Side;
import io.turntabl.orderservice.dtos.*;
import io.turntabl.orderservice.exceptions.InsufficientWalletBalanceException;
import io.turntabl.orderservice.exceptions.OrderNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
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


    /**
     * This listener listens for new orders and begins the validation process
     * @param message refers to incoming message from Order Created Event
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {

        log.info("New Order to be processed {}", message.toString());

        /*
         *  1. Verify Order Exists in Database and Delegate based on SIDE
         */
        Order order = orderRepository.findById(message.toString()).
                orElseThrow(() -> new OrderNotFoundException("Order with ID "+ message +" tobe validated and processed does not exist."));

        ExchangeDto exchangeOneDetails = getExchangeDetailsFromHash(ExchangeName.EXCHANGE_ONE);
        ExchangeDto exchangeTwoDetails = getExchangeDetailsFromHash(ExchangeName.EXCHANGE_TWO);

        if (exchangeOneDetails.isActive() || exchangeTwoDetails.isActive()) {
            switch (order.getSide()) {
                case BUY:
                    buyOperation(order, exchangeOneDetails, exchangeTwoDetails);
                    break;
                case SELL:
                    sellOperation(order, exchangeOneDetails, exchangeTwoDetails);
                    break;
            }
        } else {
            //Todo: Update order Status to PENDING with Message No Active Exchanges so a background process will process this if exchanges become available

        }

    }

    @Transactional
    void buyOperation(Order receivedOrder, ExchangeDto exchangeOne, ExchangeDto exchangeTwo) {
        /*0. TODO: Verify Daily Buy Limit isnt up.
         * 1. Verify User has sufficient funds
         * 2. Buy-executions should be in order by the lowest price in the market
         * 3. Executions to exchange should be bound to single order
         * NB: Keep track of execution price and order
         */
        try{

            Order finalReceivedOrder = receivedOrder;
            Wallet wallet = walletRepository.findById(receivedOrder.getUserId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet information for " + finalReceivedOrder.getUserId() + " not found", finalReceivedOrder.getId()));


            // Sufficient Bal Check
            if (wallet.getBalance() >= receivedOrder.getQuantity() * receivedOrder.getPrice() ){
                    // Reach into Search for sum of quantities available that matches the quantity to be traded
                    List<? extends Ticker> availableFinancialProductsOnMarket = findAppropriateTickerInformationFromOrder(receivedOrder, exchangeOne,exchangeTwo);
                    // Publish information to exchange
                    int quantitySent = 0;
                    for (Ticker marketProductForSale : availableFinancialProductsOnMarket) {
                        // 1. Get to know the exchange
                      ExchangeDto exchange = exchangeOne.getBaseUrl().equalsIgnoreCase(marketProductForSale.getExchangeURL()) ? exchangeOne : exchangeTwo;

                      // 2. TODO: Verify Quantities are publishable to exchange
                        if (quantitySent < receivedOrder.getQuantity() )   {

                            receivedOrder.setStatus(OrderStatus.PROCESSING);
                            receivedOrder.setStatusInfo("Order partially Processed");
                            receivedOrder = orderRepository.save(receivedOrder);

                            int quantityToSend = receivedOrder.getQuantity() - quantitySent;

                            if(marketProductForSale.getQuantity() > quantityToSend ){
                                // Reduce Account Balance with Corresponding Amount
                                wallet = updateBalanceOfClientsWallet(wallet, (quantityToSend * marketProductForSale.getPrice()));
                                receivedOrder = updateOrderStatusForQuantityProcessed(receivedOrder,quantityToSend);

                                sendOrderToExchange(
                                        receivedOrder,
                                        exchange,
                                        quantityToSend
                                );
                                quantitySent += quantityToSend ;

                            }else {
                                wallet = updateBalanceOfClientsWallet(wallet, marketProductForSale.getQuantity() * marketProductForSale.getPrice());
                                receivedOrder = updateOrderStatusForQuantityProcessed(receivedOrder, marketProductForSale.getQuantity());

                                sendOrderToExchange(
                                        receivedOrder,
                                        exchange,
                                        marketProductForSale.getQuantity()
                                );

                                quantitySent += marketProductForSale.getQuantity();

                            }
                        }else {
                         receivedOrder.setStatus(OrderStatus.PROCESSED);
                         receivedOrder.setStatusInfo("Order processed");
                         receivedOrder = orderRepository.save(receivedOrder);
                        }
                    }

                }else{

                throw new InsufficientWalletBalanceException("Wallet balance is insufficient for the order");
            }

        }catch (WalletNotFoundException | InsufficientWalletBalanceException exception){
            receivedOrder.setStatus(OrderStatus.INVALID);
            receivedOrder.setStatusInfo(exception.getMessage());
            orderRepository.save(receivedOrder);
        }

    }

    private Order updateOrderStatusForQuantityProcessed(Order receivedOrder, int quantityToProcess) {

        receivedOrder.setQuantityProcessed(receivedOrder.getQuantityProcessed() + quantityToProcess);
        return orderRepository.save(receivedOrder);

    }


    private void sellOperation(Order receivedOrder,ExchangeDto exchangeDto, ExchangeDto exchangeTwo) {

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

    private ExchangeDto getExchangeDetailsFromHash(ExchangeName exchangeName){
        String exchangeInfo = hashOperations.get(exchangeName.name(),exchangeName.name());
        return  gson.fromJson(exchangeInfo, ExchangeDto.class);
    }

    private Wallet updateBalanceOfClientsWallet(Wallet wallet, double amountToDebitClientAccount) {
        wallet.setBalance(wallet.getBalance() - amountToDebitClientAccount);
        return walletRepository.save(wallet);
    }

    private MarketDataDto getMarketDataFromHash(String ticker, String exchangeId){
        String dataKey = ticker+"_"+exchangeId;
        return gson.fromJson(hashOperations.get(dataKey, dataKey), MarketDataDto.class);
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


}
