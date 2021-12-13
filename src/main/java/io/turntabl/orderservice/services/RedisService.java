package io.turntabl.orderservice.services;

import io.turntabl.orderservice.dtos.ExchangeDto;
import io.turntabl.orderservice.dtos.MarketDataDto;
import io.turntabl.orderservice.enums.ExchangeName;

public interface RedisService {
    void convertAndSendToCreateOrderTopic(String orderId);
    void convertAndSendToUpdateOrderTopic(String orderId);
    ExchangeDto getExchangeDetailsFromHash(ExchangeName exchangeName);
    MarketDataDto getMarketDataFromHash(String ticker, String exchangeId);
}
