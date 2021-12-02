package io.turntabl.orderservice.services.impl;

import com.google.gson.Gson;
import io.turntabl.orderservice.constants.ExchangeName;
import io.turntabl.orderservice.constants.OrderStatus;
import io.turntabl.orderservice.constants.Side;
import io.turntabl.orderservice.dtos.ExchangeDto;
import io.turntabl.orderservice.dtos.MarketDataDto;
import io.turntabl.orderservice.dtos.OrderDto;
import io.turntabl.orderservice.dtos.PortfolioDto;
import io.turntabl.orderservice.models.Order;
import io.turntabl.orderservice.models.Wallet;
import io.turntabl.orderservice.repositories.OrderRepository;
import io.turntabl.orderservice.repositories.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RedisMessageListenerImpl implements MessageListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Gson gson;

    @Autowired
    private HashOperations<String, String, String> hashOperations;

    @Autowired
    private WalletRepository walletRepository;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("{}", message.toString());

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

        if (receivedOrder.getSide() == Side.BUY) {
            buyOperation(receivedOrder, exchangeOneData, exchangeTwoData);
        }else {
            sellOperation(receivedOrder, exchangeOneData, exchangeTwoData);
        }

    }

    private void buyOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData) {

        double totalBuyLimit = exchangeOneData.getBuyLimit() + exchangeTwoData.getBuyLimit();

        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();

        // if side is sell, check if user owns more or equal to the quantity of products being sold
        Wallet wallet = walletRepository.findByUserId(receivedOrder.getUserId()).orElse(new Wallet());

        if (wallet.getBalance() < price) {
            return;
        }

        //perform limit checking on buy
        if (receivedOrder.getQuantity() > totalBuyLimit) {
            return;
        }

        double buyPrice = Math.min(exchangeOneData.getBuyLimit(), exchangeTwoData.getBuyLimit());



    }

    private void sellOperation(Order receivedOrder, MarketDataDto exchangeOneData, MarketDataDto exchangeTwoData) {
        double totalSellLimit = exchangeOneData.getSellLimit() + exchangeTwoData.getSellLimit();

        double price = receivedOrder.getPrice() * receivedOrder.getQuantity();

        // if side is sell, check if user owns more or equal to the quantity of products being sold
        Wallet wallet = walletRepository.findByUserId(receivedOrder.getUserId()).orElse(new Wallet());

        //perform limit checking on sell
        if (receivedOrder.getQuantity() > totalSellLimit) {
            return;
        }

        Optional<PortfolioDto> portfolio = wallet.getPortfolios()
                .stream()
                .filter(x -> x.getTicker().equalsIgnoreCase(receivedOrder.getTicker()) && x.getQuantity() >= receivedOrder.getQuantity())
                .findFirst();

        if (portfolio.isEmpty()) {
            return;
        }



    }
}
